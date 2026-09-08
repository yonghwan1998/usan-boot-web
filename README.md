# 우리 동네 부동산 (usan)

> 지도 기반으로 내 주변 중개사무소 정보를 한눈에 확인할 수 있는 **Spring Boot 기반 웹 서비스**  
> 상가, 건물주, 중개인, 임차인을 빠르게 연결하는 **지역 중심형 지도 기반 플랫폼**

---

## 🚀 프로젝트 개요

| 항목 | 내용                                                        |
|------|-----------------------------------------------------------|
| 프로젝트명 | 우리 동네 부동산 (usan)                                          |
| 주요 목적 | 지도 기반 중개사무소 확인 및 매물 소유자와의 빠른 매칭                           |
| 주요 대상 | 건물주 · 상가주인 · 임대사업자 · 중개인                                  |
| 기술 스택 | Java 17, Spring Boot 3.5.7, Gradle, JPA, MySQL, Thymeleaf |
| 배포 환경 | AWS EC2 (Ubuntu), Nginx                                   |
| 저장소 구조 | `com.usanmap.usan`                                        |

---

## 🧩 주요 기능

- **지도 기반 중개사무소 탐색**  
  네이버지도 API 기반 주변 중개사무소 시각화
- **매물 등록 및 관리**

  매물 등록·수정·삭제와 중개사 공유
- **관심 지역 설정**

  행정경계 기반 관심 지역 저장과 주변 정보 제공
- **크레딧 및 결제**

  크레딧 충전·내역과 매물 전송 비용 처리

---

## 🧱 프로젝트 구조

```text
src/main/java/com/usanmap/usan/
├── controller/   요청, API 및 화면 응답
├── service/      비즈니스 로직과 트랜잭션
├── repository/   Spring Data JPA 데이터 접근
├── dto/          요청·응답·화면 데이터
├── entity/       JPA 엔티티와 상태 변경
├── batch/        중개사 데이터 적재 및 집계
└── config/       Security, JPA, 외부 연동 설정

src/main/resources/
├── templates/    Thymeleaf 화면과 fragment
└── static/       CSS, JavaScript, 이미지와 GeoJSON
```

---

## ⚙️ 실행 방법


# 1. Clone
````
git clone https://github.com/yourname/usan.git
cd usan
````

# 2. 빌드
````
./gradlew build
````

# 3. 실행
````
nohup java -jar build/libs/usan-0.0.1-SNAPSHOT.jar &
````
# 4. 로그 확인
````
tail -f nohup.out
````

## 📍 지도/행정경계

- [행정경계 GeoJSON 구축 가이드](docs/geo-boundary-build-guide.md)
