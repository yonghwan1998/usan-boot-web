# USAN 코드 컨벤션 및 아키텍처 관찰 기록

## 문서 성격

이 문서는 이상적인 Spring 표준이 아니라 현재 USAN 코드베이스에서 반복적으로 확인되는 패턴을 기록한다. `확인된 우세 패턴`과 `미결정·불일치 패턴`을 구분한다. 후자를 임의로 전역 표준화하지 않는다.

## 기술과 디렉터리

- Java 17, Spring Boot 3.5.x, Gradle을 사용한다.
- 주요 기술은 Spring MVC, Thymeleaf, Spring Security/OAuth2, Spring Data JPA, MySQL, Spring Batch, WebClient이다.
- base package는 `com.usanmap.usan`이다.
- Java 코드는 주로 `controller`, `service`, `repository`, `dto`, `entity`의 기술 계층별 패키지로 나눈다.
- 세부 책임이 큰 영역은 `batch/job/...`, `service/storage`, `common/...`과 같이 하위 패키지를 둔다.
- 화면은 `templates/layouts`, `templates/fragments`, `templates/pages/<feature>`로 구성한다.
- 정적 자원은 `static/css`, `static/js`, `static/img` 등 종류별로 둔다.

## 계층 책임

확인된 주 흐름은 `Controller -> Service -> Repository`이다.

- Controller는 요청 바인딩, 인증 사용자 확인, validation 결과 처리, Model 구성, HTTP 또는 view 응답을 담당한다.
- Service는 업무 검증, 권한 확인, 트랜잭션, Entity 상태 변경, DTO 변환을 주로 담당한다.
- Repository는 `JpaRepository` 기반 데이터 접근을 담당한다.
- 의존성 주입은 `@RequiredArgsConstructor`와 `private final` 필드를 주로 사용한다.

단, 기존 Controller와 ControllerAdvice의 Repository 직접 접근 사례가 있다. 신규 코드가 이 예외를 확대하지 않도록 검토하되, 기존 코드를 별도 요청 없이 일괄 이동하지 않는다.

## DTO와 Entity

- 요청 DTO에는 record와 Jakarta Bean Validation을 함께 사용하는 사례가 있다.
- 응답 및 view DTO에는 record와 Lombok class가 모두 사용된다.
- DTO 이름은 `*Dto`, `*Request`, `*Response`, view 성격이면 `*ViewModel`을 사용한다.
- Entity는 `@Entity`, `@Table`을 사용하며 Java 필드는 camelCase, DB 컬럼은 snake_case를 주로 사용한다.
- Long `IDENTITY` ID와 UUID/BINARY ID가 도메인에 따라 공존한다.
- Entity 변경은 의미 있는 메서드와 dirty checking을 사용하는 사례가 많다.

Entity의 캡슐화 수준은 통일되어 있지 않다. protected 생성자와 제한된 builder를 쓰는 Entity, setter와 `@Data`를 쓰는 Entity가 함께 있다. 새 Entity나 대규모 변경에서 어떤 방식을 표준화할지는 별도 결정 사항이다.

## JPA와 조회

- 모든 Repository는 Spring Data `JpaRepository` 기반이다.
- 단순 조회는 `findBy...`, `findAllBy...OrderBy...`, `existsBy...` 형태의 파생 쿼리를 주로 사용한다.
- 복잡한 조회는 `@Query`의 JPQL, DTO constructor projection, interface projection 또는 native SQL을 사용한다.
- 공간 조회와 집계에는 MySQL native SQL이 사용된다.
- 배치 또는 대량 적재에서는 `JdbcTemplate` 직접 SQL도 사용한다.
- 결제와 잔액처럼 동시성이 중요한 조회에는 비관적 락 사용 사례가 있다.
- QueryDSL 의존성과 구현 코드는 현재 없다. QueryDSL을 사용하려면 별도의 기술 도입 결정이 필요하다.

## 트랜잭션

- 트랜잭션은 Service public method에 Spring의 `org.springframework.transaction.annotation.Transactional`을 선언하는 것이 주된 방식이다.
- 조회는 `@Transactional(readOnly = true)`, 쓰기는 기본 `@Transactional`을 주로 사용한다.
- 트랜잭션 안에서 Entity 변경 메서드 또는 setter를 호출하고 JPA dirty checking에 맡기는 패턴이 있다.
- 배치 writer와 tasklet에도 작업 단위에 따라 트랜잭션을 둔다.

Controller 트랜잭션 및 Jakarta `@Transactional` 사용도 기존 코드에 있으나 우세 패턴은 아니다. 관련 코드를 수정할 때는 경계를 확인하고 혼용을 무심코 확대하지 않는다.

## 예외와 응답

- 대상 미존재나 잘못된 인자는 주로 `IllegalArgumentException`을 사용한다.
- 권한이나 처리 상태 문제는 주로 `IllegalStateException`을 사용한다.
- 일부 업무에는 별도 도메인 예외가 있다.
- 인프라 예외는 원인을 포함한 `IllegalStateException` 또는 `RuntimeException`으로 감싸는 사례가 있다.
- MVC form validation은 `BindingResult`로 같은 화면에 오류를 반환한다.

전역 예외 응답 규칙은 없다. `GlobalControllerAdvice`는 현재 공통 ModelAttribute 제공용이며 `@ExceptionHandler`를 제공하지 않는다. API별 status/body 형식과 한글·영문 메시지가 혼재하므로 기존 endpoint의 계약을 보존하고, 공통화는 별도 작업으로 다룬다.

## URL과 Controller

- 페이지 URL은 `/listings`, `/credits`, `/payments`, `/map`처럼 명사형 경로를 주로 사용한다.
- API는 `/api/...`, 지도 기능은 `/map/api/...` 형태가 보인다.
- 여러 단어로 된 경로는 kebab-case를 주로 사용한다.
- 페이지 Controller는 view 이름 또는 redirect 문자열을 반환하고 API Controller는 객체나 `ResponseEntity`를 반환한다.

다음은 통일되지 않았다.

- `/api/...`, `/<feature>/api/...`, `/map/api/...` 위치
- 리소스 단수와 복수
- `PageController`, `ApiController` suffix 적용 여부
- action URL과 HTTP method 선택
- 한 Controller에서 page와 API를 함께 제공하는 방식

새 endpoint는 같은 기능 영역의 기존 URL 계약을 우선 확인한다. 전역 URL 개편은 별도 요구 없이 수행하지 않는다.

## Java 네이밍과 스타일

- 클래스와 enum은 PascalCase를 사용한다.
- 메서드, 매개변수, 지역변수, 필드는 camelCase를 사용한다.
- 역할 이름은 `Controller`, `Service`, `Repository`, `Dto`, `Request`, `Response` suffix를 주로 사용한다.
- 도메인에서 이미 사용하는 `admCd`, `emd`, `sigungu`, `lat`, `lng`, `radiusM`, `depositManwon` 등의 이름을 유지한다.
- DB identifier는 snake_case, URL과 HTML data attribute는 kebab-case를 주로 사용한다.
- 자동 formatter, Checkstyle, Spotless, ESLint, Prettier, `.editorconfig`에 의한 전역 규칙은 현재 없다.

따라서 수정 파일의 기존 들여쓰기, import, annotation 배치와 줄바꿈을 우선하며 전체 파일을 무관하게 재포맷하지 않는다.

## Thymeleaf와 CSS

- 일반 페이지는 `layouts/layout` fragment에 title, pcSideContent, content, extraCss 슬롯을 전달한다.
- 공통 header, search-header, more-panel, bottom-nav는 fragment로 조립한다.
- 공통 CSS와 페이지·도메인별 CSS를 분리하고 Thymeleaf timestamp query로 cache busting한다.
- HTML에서는 필요한 부분에만 `th:text`, `th:if`, `th:each`, `th:field`, `th:inline="javascript"`를 적용한다.
- CSS는 BEM과 유사한 `block__element`, `block--modifier`, 상태용 `is-*`가 우세하다.
- 페이지별 namespace와 기존 utility class도 함께 사용된다.
- 결제 bridge나 다운로드 페이지처럼 공통 layout을 사용하지 않는 목적성 예외가 있다.

퍼블리셔 DOM 구조와 class를 최대한 유지한다. selector 변경 시 HTML, CSS, JavaScript 사용처를 함께 확인한다.

## JavaScript

- 공통 유틸리티는 `static/js/common.js`에 있으며 `__` prefix 함수와 `window.Common*` 객체를 사용한다.
- Naver `MarkerClustering.js`는 라이선스와 로컬 변경 기록이 있는 외부 기반 파일이다.
- 페이지 전용 로직은 현재 Thymeleaf 파일의 inline script에 많이 존재한다.
- 초기화는 `DOMContentLoaded`, 이벤트는 `addEventListener`, HTTP 호출은 native `fetch`가 주로 사용된다.
- DOM 요소가 없을 수 있는 경우 early return과 null guard를 사용한다.
- JavaScript 변수는 camelCase, 상수는 UPPER_SNAKE_CASE, data attribute와 CSS class는 kebab-case를 주로 사용한다.

다음은 통일되지 않았다.

- inline script를 별도 파일로 분리하는 기준
- single/double quote
- 함수 선언, function expression, arrow function
- fetch 오류와 비정상 status 처리
- `createElement`/`textContent`와 template literal/`innerHTML`

서버 또는 사용자 데이터를 `innerHTML`로 삽입할 때는 escaping과 XSS를 반드시 검토한다. 공통화나 대형 script 분리는 영향 범위가 큰 별도 계획으로 다룬다.

## 주석과 문서화

- Java와 공통 JavaScript의 일부 복잡한 로직에는 `@date`, `@author`, `@param`, `@return`, 처리 과정, 예외/주의 형식이 사용된다.
- HTML은 영역을 설명하는 짧은 한글 주석을 주로 사용한다.
- 복잡한 페이지 script는 단계와 목적을 설명하는 구획 주석이 있다.
- 모든 메서드에 상세 주석을 강제하는 일관된 규칙은 없다.
- 코드가 설명하는 내용을 반복하기보다 비자명한 업무 이유, 외부 제약, 변경 시 주의점을 기록한다.
- 벤더 파일의 라이선스와 기존 로컬 변경 이력을 보존한다.
- 기존 주석 처리 코드는 별도 요청 없이 대량 정리하지 않으며 신규 dead code를 주석으로 남기지 않는다.

## 테스트

- 현재 확인되는 자동 테스트는 `@SpringBootTest`, `@ActiveProfiles("test")` 기반 컨텍스트 로드 테스트 하나이다.
- 테스트 환경은 H2를 사용한다.
- 단위 테스트, MockMvc, Repository slice, fixture, Mockito에 대한 프로젝트 고유 표준은 아직 없다.
- 변경 위험에 맞는 테스트를 추가할 수 있지만 새로운 테스트 방식을 전역 표준처럼 선언하지 않는다.
- 자동 테스트가 부족한 영역은 실행한 수동 검증과 검증하지 못한 항목을 구분해 보고한다.

## Git과 커밋

- 프로젝트 문서는 Conventional Commits를 요구하며 실제 이력에서도 소문자 type 메시지가 다수이다.
- 일반적인 제목은 `<type>: <한국어 요약>`이다.
- 실제 이력에는 비정형, 영어 제목, 대문자 type, merge 및 PR 번호 포함 메시지도 존재한다.
- `style`은 이력에서 사용되지만 기존 문서의 type 목록에는 없다.
- 커밋 Agent의 세부 규칙은 `.agents/commit.md`를 따른다.
