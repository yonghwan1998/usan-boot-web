# usan 프로젝트 개발 가이드

## 프로젝트 개요
- 프로젝트명: usan (우리 동네 부동산)
- 목적: 지도 기반으로 내 주변 중개사 찾기 및 중개사에게 내 매물 보내기 서비스 제공
- 주요 사용자:
    - 건물주 / 임대사업자
    - 공인중개사

## 현재 개발 방향
- MVC 패턴에 집중
- 과도한 설계보다 빠른 개발 우선

## 기술 스택
- Java 17+
- Spring Boot 3.5.x
- Gradle
- Thymeleaf
- JPA + QueryDSL
- MySQL
- IDE: IntelliJ
- 인프라: AWS EC2

## 패키지 규칙
- base package: com.usanmap.usan
- 현재 구조 최대한 유지
- MVC 구조 유지

## 개발 원칙
- 컨트롤러는 최대한 얇게 유지
- 비즈니스 로직은 서비스에 위치
- Repository는 데이터 접근만 담당
- QueryDSL은 복잡한 조회에서만 사용
- 불필요한 추상화 금지

## Thymeleaf 규칙
- templates 디렉토리 사용
- static 리소스는 css/js/img로 분리
- 퍼블리셔 HTML 구조 최대한 유지
- 필요한 부분만 th:* 적용

## API 규칙
- RESTful 구조 지향
- 페이지 컨트롤러와 API 분리
- DTO는 필요할 때만 분리

## Git 규칙
- Conventional Commits 사용
    - feat:
    - fix:
    - refactor:
    - docs:
    - perf
    - test
    - chore
    - revert

## Claude Code 응답 규칙
- 먼저 변경 이유 설명
- 최소 변경 코드 제안
- 전체 구조 갈아엎지 말 것
- 기존 코드 스타일 유지
- 불확실한 내용은 명시
- 여러 방법 있으면 하나 추천

## 목표
1. 안정성
2. 빠른 개발 속도
3. 유지보수 용이성
4. 최소한의 변경