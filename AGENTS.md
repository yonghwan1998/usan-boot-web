# USAN 개발 Agent 운영 규칙

## 목적

이 문서는 USAN 저장소에서 작업하는 모든 Agent가 따르는 최상위 규칙이다. 세부 코드 관행은 `.agents/convention.md`, 역할별 절차는 `.agents/planner.md`, `.agents/coder.md`, `.agents/reviewer.md`, `.agents/commit.md`를 따른다.

규칙이 충돌할 때는 다음 우선순위를 적용한다.

1. 사용자의 현재 요청
2. 이 `AGENTS.md`
3. 역할별 Agent 문서
4. `.agents/convention.md`
5. 변경 대상과 가장 가까운 기존 코드의 패턴
6. `CLAUDE.md` 등 기존 프로젝트 문서

## 프로젝트 기준

- 단일 Gradle 기반 Spring Boot 애플리케이션이다.
- Java 17과 Spring Boot 3.5.x를 사용한다.
- 실제 base package는 `com.usanmap.usan`이다.
- 현재의 기술 계층형 패키지 구조와 MVC 구조를 유지한다.
- 빠른 개발, 안정성, 유지보수성, 최소 변경을 우선한다.
- 일반적인 Spring 권장사항보다 이 저장소에서 실제로 확인되는 인접 코드 패턴을 먼저 따른다.
- 퍼블리셔가 작성한 HTML DOM 구조와 CSS class는 요구사항 수행에 꼭 필요한 경우가 아니면 변경하지 않는다.

## 공통 작업 원칙

- 작업 전에 관련 Controller, Service, Repository, DTO, Entity, template, CSS, JavaScript, 설정과 테스트를 확인한다.
- 요구사항을 만족하는 가장 작은 변경 범위를 선택한다.
- 요청과 직접 관계없는 리팩터링, 이름 변경, 패키지 이동, 포맷 일괄 변경을 함께 수행하지 않는다.
- 기존 코드의 불일치를 발견해도 작업 범위 밖에서는 임의로 통일하지 않는다.
- 새 라이브러리, 계층, 인터페이스, 공통 추상화는 명확한 필요와 승인 없이 도입하지 않는다.
- QueryDSL은 현재 빌드와 코드에서 사용되지 않는다. 문서의 언급만 근거로 도입하거나 사용 중이라고 가정하지 않는다.
- 비밀값과 로컬 파일을 커밋하지 않는다. 특히 `.env`, 운영 설정, IDE 파일, `build/`, 업로드 파일, 생성 데이터 파일을 주의한다.
- 사용자 변경과 무관한 기존 작업을 덮어쓰거나 되돌리지 않는다.
- 구현 후 변경 위험에 비례하여 테스트, 빌드 또는 수동 검증을 수행하고 결과를 사실대로 보고한다.

## 미결정 패턴 처리

다음 항목은 현재 코드에서 서로 다른 방식이 공존하므로 전역 표준으로 간주하지 않는다.

- Controller의 Repository 직접 접근
- Controller와 Service 사이의 트랜잭션 경계
- Spring 및 Jakarta `@Transactional` 사용
- DTO의 record 또는 Lombok class 선택
- Entity의 setter, `@Data`, builder, 정적 팩토리 및 변경 메서드 사용
- Entity, DTO, `Map<String, Object>` 반환 방식
- API base path, 리소스 단수·복수, action URL
- API 오류 status와 body 포맷
- Page/API Controller 이름 구분
- JavaScript의 inline/외부 파일 분리 기준
- 테스트 유형과 fixture 구성
- Conventional Commits의 `style` type 포함 여부

해당 선택이 필요한 경우에는 가장 가까운 기능의 기존 구현을 근거로 삼고, 선택이 영향 범위를 넓히거나 향후 표준을 사실상 결정한다면 사용자에게 결정 사항으로 알린다.

## Agent 작업 흐름

복잡한 변경은 다음 흐름을 기본으로 한다.

1. Planner가 요구사항, 영향 범위, 기존 근거, 구현 및 검증 계획을 작성한다.
2. Coder가 승인된 계획과 컨벤션에 맞춰 최소 범위로 구현한다.
3. Reviewer가 기능, 회귀, 보안, 데이터, 컨벤션을 검토한다.
4. Commit Agent가 실제 변경사항을 근거로 커밋 메시지를 제안한다.

단순하고 국소적인 변경은 역할을 순차적으로 흉내 내되 불필요한 문서성 산출물을 만들지 않아도 된다.

## 기본 검증 명령

- 전체 테스트: `./gradlew test`
- 빌드가 필요한 경우: `./gradlew build`
- 변경 확인: `git diff --check` 및 `git diff`

현재 자동 테스트는 애플리케이션 컨텍스트 로드 수준이므로 테스트 통과만으로 기능 검증이 완료되었다고 단정하지 않는다.
