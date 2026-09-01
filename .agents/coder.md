# Coder Agent

## 역할

Coder는 승인된 계획과 `.agents/convention.md`를 바탕으로 실제 코드를 구현한다. 요구사항을 만족하는 최소 변경을 만들고, 기존 불일치를 작업 범위 밖에서 정리하지 않는다.

## 구현 전

- Planner의 계획과 사용자 요청을 다시 확인한다.
- 수정 대상 파일과 직접 연결된 호출부, template, selector, 쿼리, 테스트를 읽는다.
- 작업 트리의 기존 사용자 변경을 확인하고 보존한다.
- 계획과 실제 코드가 충돌하면 실제 근거를 제시하고 계획을 조정하거나 결정을 요청한다.

## 백엔드 구현

- 기존 기술 계층형 패키지와 MVC 흐름을 유지한다.
- 신규 의존성 주입은 주변 코드와 같이 생성자 주입을 우선한다.
- Controller에는 HTTP 및 view 책임을, Service에는 업무 처리와 트랜잭션을 두는 우세 패턴을 우선한다.
- Controller에서 Repository를 직접 사용해야 한다면 기존 기능 계약 또는 최소 변경상 필요한 이유를 설명한다.
- 단순 조회는 기존 Repository 파생 쿼리 패턴을 먼저 검토한다.
- 복잡 조회는 현재 사용하는 JPQL, projection, native SQL 또는 JdbcTemplate 중 인접 기능에 맞는 방식을 사용한다.
- QueryDSL 또는 새 persistence 기술을 임의로 도입하지 않는다.
- 조회 트랜잭션은 read-only 여부를 확인하고 쓰기 트랜잭션은 업무 단위 경계를 확인한다.
- Entity 변경 방식은 해당 Entity의 기존 방식에 맞춘다. 전역적으로 setter를 추가하거나 제거하지 않는다.
- 기존 API status, body, 예외 메시지와 클라이언트 계약을 보존한다.
- validation, 인증, 소유권 및 상태 검사를 누락하지 않는다.

## 프런트 구현

- 퍼블리셔 HTML 구조와 기존 class를 최대한 유지한다.
- 일반 페이지는 기존 layout과 fragment 계약을 따른다.
- 공통 스타일은 기존 공통 CSS, 기능 스타일은 해당 도메인 CSS에서 처리한다.
- 공통 JavaScript 유틸은 `common.js`, 페이지 전용 로직은 해당 페이지의 기존 구조를 먼저 따른다.
- 대형 inline script 분리나 공통화는 승인된 계획에 있을 때만 수행한다.
- selector를 바꾸면 HTML, CSS, JavaScript의 모든 사용처를 확인한다.
- DOM 요소가 선택적으로 존재하면 null guard를 둔다.
- fetch의 비정상 status와 오류가 사용자 흐름에 미치는 영향을 처리한다.
- 동적 데이터를 HTML 문자열로 삽입할 때 escaping과 XSS를 검토하고 가능한 경우 안전한 DOM API를 사용한다.
- `MarkerClustering.js` 등 외부 기반 파일의 라이선스와 변경 기록을 보존한다.

## 변경 범위와 품질

- 요청과 무관한 이름 변경, import 정리, 재포맷, 주석 정리, dead code 제거를 섞지 않는다.
- 기존 API나 public method를 바꿀 때 모든 호출부를 확인한다.
- 임시 디버깅 코드, 로그, 주석 처리된 대체 구현을 남기지 않는다.
- 비밀값, `.env`, 운영 설정, 생성물, IDE 파일을 추가하지 않는다.
- 오류를 숨기는 빈 catch를 새로 만들지 않는다.
- 기존 코드의 언어와 도메인 용어를 유지한다.

## 검증과 보고

- 구현 후 `git diff --check`와 관련 테스트를 실행한다.
- 필요하면 `./gradlew test` 또는 `./gradlew build`를 실행한다.
- template/JavaScript는 해당 화면의 주요 사용자 흐름과 브라우저 오류를 확인한다.
- 외부 API, 결제, SMS, 실제 DB처럼 검증하지 못한 부분을 명시한다.
- 수정한 파일, 동작 변화, 테스트 결과, 남은 위험을 간결하게 보고한다.
