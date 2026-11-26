# 문자 링크 방문 추적

## 데이터 흐름과 배포

1. `ListingSendService.prepare`가 발송 묶음과 수신자 이력을 저장하면서 6자 토큰을 발급한다.
2. 같은 매물은 행 잠금으로 동시 발급을 직렬화한다. 같은 매물의 기존 이력과 현재 발송 내 중복을 확인해 재생성하며, DB `(listing_id, tracking_token)` UNIQUE 제약으로도 보호한다.
3. `SmsService`가 이력을 다시 조회해 `/l/{publicId}?t={token}`을 수신자별 본문에 넣는다.
4. GET 접근 시 매물과 토큰을 함께 확인하고 `listing_link_visit`에 접근마다 INSERT한다. HEAD, 없는 토큰, 형식 오류, 다른 매물의 토큰은 기록하지 않는다.
5. 발송 내역 목록과 상세 화면은 로그인한 발송자 소유의 이력만 집계한다. 추적 도입 전 이력은 분모에서도 제외한다.

운영은 `ddl-auto=validate`이므로 **배포 전에 `scripts/sms-link-visits-schema.sql`을 적용**한다.
SQL은 최초 1회 적용용이며 기존 이력에는 토큰을 소급 생성하지 않는다. MySQL 실제 적용은 별도 검증해야 한다.
토큰은 전화번호·수신자 PK를 인코딩한 값이 아니라 SecureRandom 기반 URL-safe 무작위 문자열이다.
문자 API는 [SENS 수신자별 content](https://api.ncloud-docs.com/docs/en/sens-sms-send)를 사용한다.
기존 테스트 번호 대체 발송 정책은 유지했으므로 실제 수신자 번호로 전환하는 작업과 혼동하지 않는다.

## 지표의 의미

- 방문 여부: 해당 수신자에게 발급된 링크로 GET 접근이 있었는지.
- 방문 횟수·최초·최근 시각: 접근 이벤트 기준. 재방문을 원본 이벤트로 보존한다.
- 링크 방문율: `방문한 고유 수신 이력 수 / 토큰이 발급된 수신 이력 수 × 100`.
- 분모에는 접수 실패·최종 발송 실패도 포함된다. 전체 발송 시도 대비 지표이며 실제 수신 성공 대비 지표와 구별한다.
- 링크 미리보기·자동 점검·전달된 링크의 접근도 포함될 수 있어 실제 사람의 클릭 또는 문자 열람을 보장하지 않는다.
- 방문 기록 DB 오류는 로그에 매물 ID만 남기고 기존 매물 조회를 계속 제공한다. 이 경우 집계가 누락될 수 있다.
- 응답은 `no-store`, `Referrer-Policy: no-referrer`로 캐시와 외부 리퍼러를 통한 토큰 노출을 줄인다. 운영 접근 로그에서도 쿼리 문자열 마스킹을 고려한다.

## 조건별 비교 확장

기존 발송 API의 `listingId`, `brokerCodes`는 유지한다. 선택적으로 다음 조건을 추가할 수 있다.

```json
{
  "listingId": 1,
  "brokerCodes": ["00000000-0000-0000-0000-000000000001"],
  "selectionCriteria": {"radiusM": 1000, "listingTypes": ["아파트", "원룸"]}
}
```

실제 수신자 집합은 기존처럼 `brokerCodes`로 결정된다. `selectionCriteria`는 발송 당시 선택 조건의 기록이며 서버에서 수신자를 다시 필터링하는 입력은 아니다.
현재 지도 화면은 거리·유형을 보내고, 충전 후 재발송을 위한 세션 저장에도 당시 조건을 보존한다.
유형은 정렬·중복 제거해 묶음에 저장한다. 조건 미제공은 NULL로 남아 기존 클라이언트와 호환된다.
추후 조건이 추가되면 발송 묶음에 해당 조건의 스냅샷을 추가하여 이벤트 이력과 연결할 수 있다.

조건별 기초 집계 예시:

```sql
SELECT b.selection_radius_m, b.selection_listing_types,
       COUNT(DISTINCT h.id) AS tracked_recipients,
       COUNT(DISTINCT v.history_id) AS visited_recipients,
       100.0 * COUNT(DISTINCT v.history_id) / NULLIF(COUNT(DISTINCT h.id), 0) AS visit_rate
FROM listing_send_batch b
JOIN listing_send_history h ON h.batch_id = b.id AND h.user_id = b.user_id
LEFT JOIN listing_link_visit v ON v.history_id = h.id
WHERE b.user_id = :user_id AND h.tracking_token IS NOT NULL
  AND b.requested_at >= :from_time AND b.requested_at < :until_time
GROUP BY b.selection_radius_m, b.selection_listing_types;
```

비교 시 동일한 관찰 기간을 사용한다. 10%→15%는 5%p 상승·상대 50% 상승이다.
기존 비율이 0이면 상대 상승률은 계산하지 않는다. 조건 외에 시점·매물·수신자 차이가 있어 관측 방문율 차이만으로 조건의 인과 효과나 문자 열람 확률을 단정하지 않는다.
현재 범위는 방문 기록·조회와 조건 저장이며, 비교 실험이나 자동 재발송은 수행하지 않는다.
