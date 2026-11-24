# 중개사 매물 집계 배치

`POST /broker/batch/run-broker-property-count`의 URL과 집계 조건은 유지한다.
매물이 없는 중개사도 유형별 0건으로 포함하며, 알려지지 않은 유형은 유형별 건수에 포함하지 않는다.

## 처리 방식

- 중개사 BINARY(16) PK 순서로 1,000개씩 조회하고 해당 중개사만 매물 집계한다.
- 전용 연결의 임시 테이블을 운영 테이블과 같은 구조로 생성한다. 별도 영구 테이블 마이그레이션은 없다.
- 페이지별 JDBC batch insert 후 커밋한다. 애플리케이션은 전체 집계 결과를 보관하지 않는다.
- 기존 public_id는 보존한다. 신규 중개사는 기존 운영 ID와 중복되지 않는 ID를 발급하고, 작업 테이블 내 충돌 시 페이지를 롤백하고 최대 5회 시도한다.
- 작업 테이블 건수 검증 후 DELETE와 INSERT SELECT를 하나의 트랜잭션으로 실행한다. 실패하면 운영 데이터 변경을 롤백한다.
- 비어 있는 원본은 실패 처리한다. 정상적인 전체 비우기는 별도 운영 작업이다.
- MySQL GET_LOCK을 같은 물리 연결에서 획득/해제하여 수동·스케줄·다중 인스턴스의 실제 집계 중복 실행을 막는다. 잠금 획득 실패 시 해당 Job은 FAILED로 끝난다.
- 실행 중 수정되는 원본은 페이지마다 다른 시점으로 반영될 수 있다. 전체 시점 일치가 필요하면 수집 작업과 실행 시간을 분리한다.
- 연결은 작업 전체 동안 하나를 점유한다. 마지막 전체 반영은 테이블 규모에 비례하는 트랜잭션이며, DB 부하와 잠금 대기는 남는다.
- created_at/updated_at/deleted_at 재설정은 기존 동작을 유지한다. total_cnt 등 생성 컬럼은 복사 대상에서 제외하여 DB가 계산한다.

## 배포 전 DB 확인

운영 DB에는 자동으로 인덱스를 생성하지 않는다. 다음을 확인한다.

```sql
SHOW CREATE TABLE broker_info;
SHOW CREATE TABLE listing_info;
SHOW CREATE TABLE broker_property_count;
SHOW INDEX FROM listing_info;
```

broker_property_count는 롤백 가능한 InnoDB여야 하며 코드에서도 검증한다.
DB 계정에 CREATE TEMPORARY TABLES 권한이 필요하다.
listing_info에 broker_code로 시작하는 인덱스가 필요하다. 기존 인덱스와 실행 계획을 확인한 후
필요하면 (broker_code, listing_type) 복합 인덱스를 운영 절차에 따라 추가한다.
인덱스가 없으면 페이지마다 전체 매물 스캔이 발생할 수 있다.

운영과 같은 MySQL에서 CREATE TEMPORARY TABLE ... LIKE, GET_LOCK/RELEASE_LOCK,
생성 컬럼/제약조건 호환성, 실패 시 롤백, 대표 데이터의 EXPLAIN과 실행 시간을 검증한다.
H2 테스트는 집계·페이지 경계·공유 ID 보존·최종 반영 실패 시 기존 데이터 보존을 검증하지만
MySQL 전용 잠금/임시 테이블 DDL과 424만 건 성능은 검증하지 않는다.
