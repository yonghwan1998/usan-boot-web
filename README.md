# 우리 동네 부동산 (USAN)

지도에서 주변 중개사무소와 매물을 확인하고, 매물 소유자와 중개사를 연결하는 지역 기반 부동산 플랫폼입니다.

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 서비스명 | 우리 동네 부동산 (USAN) |
| 주요 사용자 | 건물주, 상가 소유자, 임대사업자, 중개사 |
| 주요 기능 | 중개사무소 탐색, 매물 관리·공유, 관심 지역, 크레딧·결제 |
| 애플리케이션 | Java 17, Spring Boot 3.5.7, Gradle |
| 화면 | Spring MVC, Thymeleaf, HTML/CSS/JavaScript |
| 데이터 | Spring Data JPA, MySQL, Hibernate Spatial, JTS |
| 운영 환경 | AWS EC2 (Ubuntu), Nginx |

## 주요 기능

- **지도 기반 중개사무소 탐색**
  - 네이버 지도에서 주변 중개사무소와 매물을 표시합니다.
  - 반경 조회, 지도 영역 조회, 마커 클러스터링과 행정구역 정보를 제공합니다.
- **매물 등록 및 관리**
  - 매물을 등록·수정하고 사진을 관리합니다.
  - 공개 링크를 통해 매물과 중개사 정보를 공유할 수 있습니다.
- **매물 전송**
  - 선택한 중개사에게 매물 정보를 SMS로 전송합니다.
  - 전송 건수에 따라 크레딧을 차감하고 전송 이력을 저장합니다.
- **관심 지역**
  - 시·도, 시·군·구, 읍·면·동 단위로 관심 지역을 저장합니다.
  - 저장한 관심 지역을 지도 초기 위치와 지역 정보 조회에 사용합니다.
- **회원 및 인증**
  - 일반 회원가입·로그인과 Google, Kakao, Naver OAuth2 로그인을 지원합니다.
  - 휴대전화 인증, 아이디 찾기, 임시 비밀번호 발급과 비밀번호 변경을 제공합니다.
- **크레딧 및 결제**
  - 크레딧 잔액·사용 내역과 충전 주문을 관리합니다.
  - 헥토파이낸셜 결제와 무통장 입금 신청·승인 흐름을 지원합니다.
- **데이터 관리**
  - Spring Batch와 Apache POI를 이용해 중개사 Excel 데이터를 적재합니다.
  - 중개사 매물 수 집계, 행정경계 적재와 지역 통계 갱신 기능을 제공합니다.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.7, Spring MVC, WebFlux `WebClient` |
| Security | Spring Security, OAuth2 Client |
| View | Thymeleaf, HTML, CSS, JavaScript |
| Persistence | Spring Data JPA, MySQL, Hibernate Spatial, JTS |
| Batch | Spring Batch, Apache POI |
| External API | Naver Maps·Geocode, Kakao API, NCP SENS, 헥토파이낸셜 |
| File | 로컬 파일 저장소, multipart 업로드 |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| Test | JUnit 5, Mockito, MockMvc, H2 |

## 프로젝트 구조

```text
src/main/java/com/usanmap/usan/
├── controller/   화면과 API 요청 처리
├── service/      비즈니스 로직과 트랜잭션
├── repository/   Spring Data JPA 데이터 접근
├── dto/          요청·응답·화면 데이터
├── entity/       JPA 엔티티와 상태 모델
├── batch/        중개사 데이터 적재와 집계
├── client/       외부 API 클라이언트
├── security/     인증 사용자와 보안 지원
├── config/       Security, JPA, WebClient와 파일 설정
├── common/       공통 도메인·JPA 지원 코드
└── util/         암호화, 거리, 식별자 등의 유틸리티

src/main/resources/
├── templates/    Thymeleaf layout, fragment와 기능별 화면
├── static/       CSS, JavaScript, 이미지, GeoJSON과 다운로드 파일
└── application.yml

src/test/java/com/usanmap/usan/
├── controller/   지도 조회 API MockMvc 테스트
├── service/      서비스 단위 테스트
└── UsanApplicationTests.java
```

## 로컬 실행

### 1. 준비 사항

- JDK 17
- MySQL
- 외부 연동 기능을 사용할 계정과 API 키

### 2. 저장소 받기

```bash
git clone https://github.com/yonghwan1998/usan-boot-web.git
cd usan-boot-web
```

### 3. 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다. `.env`에는 비밀값이 포함되므로 Git에 커밋하지 않습니다.

기본 실행에 필요한 데이터베이스 설정 예시는 다음과 같습니다.

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/usan?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=local_user
SPRING_DATASOURCE_PASSWORD=local_password
USAN_BASE_URL=http://localhost:8080
```

기능별 주요 환경변수는 다음과 같습니다.

| 기능 | 환경변수 |
| --- | --- |
| Google 로그인 | `GOOGLE_OAUTH2_CLIENT_ID`, `GOOGLE_OAUTH2_CLIENT_SECRET` |
| Kakao 로그인·주소 검색 | `KAKAO_OAUTH2_CLIENT_ID`, `KAKAO_OAUTH2_CLIENT_SECRET`, `KAKAO_REST_API_KEY` |
| Naver 로그인 | `NAVER_OAUTH2_CLIENT_ID`, `NAVER_OAUTH2_CLIENT_SECRET` |
| Naver 지도·주소 변환 | `NAVER_MAP_CLIENT_ID`, `NAVER_MAP_CLIENT_SECRET`, `NAVER_MAP_DARK_THEME_CUSTOM_ID` |
| NCP SENS 문자 발송 | `NCP_ACCESS_KEY`, `NCP_SECRET_KEY`, `NCP_SENS_SERVICE_ID`, `NCP_SENS_FROM` |
| 무통장 입금 | `USAN_BANK_NAME`, `USAN_BANK_NUMBER`, `USAN_BANK_HOLDER` |
| 헥토파이낸셜 | `HECTO_MCHT_ID`, `HECTO_HASH_KEY`, `HECTO_ENC_KEY`, `HECTO_TEST_MODE`, `HECTO_MCHT_NAME`, `HECTO_MCHT_ENAME` |

`NCP_SENS_FROM`에는 NCP SENS에 등록·승인된 발신번호를 하이픈 없이 입력합니다.

### 4. 실행

```bash
./gradlew bootRun
```

기본 접속 주소는 `http://localhost:8080`입니다.

## 테스트와 빌드

전체 테스트를 실행합니다.

```bash
./gradlew test
```

실행 가능한 JAR을 생성합니다.

```bash
./gradlew clean build
```

```bash
java -jar build/libs/usan-0.0.1-SNAPSHOT.jar
```

현재 자동 테스트는 애플리케이션 컨텍스트 로드, 관심 지역 서비스 단위 테스트와 지도 조회 API의 standalone MockMvc 테스트를 포함합니다.

## Git 작업 흐름

1. `dev`를 최신화합니다.
2. `dev`에서 `<type>/<kebab-case-summary>` 형식의 작업 브랜치를 생성합니다.
3. 구현과 검증 후 Conventional Commit 형식으로 커밋합니다.
4. 작업 브랜치를 push하고 `dev` 대상 Pull Request를 생성합니다.
5. 배포할 변경은 `dev`에서 `main`으로 Pull Request를 생성합니다.

`main`과 `dev`에는 직접 변경하거나 커밋하지 않습니다. 세부 기준은 [AGENTS.md](AGENTS.md)와 [.agents/commit.md](.agents/commit.md)를 따릅니다.

## 관련 문서

- [행정경계 GeoJSON 구축 가이드](docs/geo-boundary-build-guide.md)
- [개발 Agent 운영 규칙](AGENTS.md)
- [코드 컨벤션과 아키텍처 관찰 기록](.agents/convention.md)
