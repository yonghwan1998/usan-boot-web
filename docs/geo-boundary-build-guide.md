# 행정경계 GeoJSON 구축 가이드 (VWorld 기반)

## 1. 개요
본 문서는 VWorld에서 제공하는 행정경계 데이터를 활용하여
Spring Boot + Naver Map 환경에서 사용할 GeoJSON 파일을 생성하는 방법을 정리한다.

---

## 2. 데이터 다운로드
아래 링크에서 행정경계 데이터를 다운로드한다.

- 시도: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30253
- 시군구: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30252
- 읍면동: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30254

### 다운로드 시 주의사항
- 반드시 **전체 압축파일(zip)** 다운로드
- 압축 해제 시 아래 파일들이 모두 존재해야 함

```
.shp  (geometry)
.shx  (index)
.dbf  (속성 데이터)
.prj  (좌표계 정보)
.cpg  (인코딩 정보, 선택)
```

👉 이 중 `.dbf`가 없으면 행정코드/이름을 사용할 수 없음

---

## 3. 프로젝트 구조

```
project-root/
  data/
    shp/
      N3A_G0010000.*   (시도)
      N3A_G0100000.*   (시군구)
      N3A_G0110000.*   (읍면동)
  scripts/
    build-geo.js
  src/main/resources/static/geo/
```

---

## 4. mapshaper 설치

```bash
npm install -g mapshaper
```

설치 확인

```bash
mapshaper -v
```

---

## 5. 데이터 확인

```bash
mapshaper data/shp/N3A_G0010000.shp -info
```

확인해야 할 필드

```
BJCD  (행정구역 코드)
NAME  (행정구역 이름)
```

---

## 6. 변환 프로세스

### 전체 흐름

```
shp → geojson → simplify → Node 분할 → static 저장
```

---

## 7. 실행 커맨드

```bash
npm run geo:build
```

---

## 8. 처리 단계 설명

### 1) 좌표계 변환

```bash
-proj wgs84
```

- Naver Map은 WGS84 좌표계 사용

---

### 2) 좌표 단순화

```bash
-simplify 8% keep-shapes
```

권장 값:

| 레벨 | 값 |
|------|----|
| 시도 | 8% |
| 시군구 | 8% |
| 읍면동 | 5% |

👉 값이 낮을수록 더 가볍지만 경계가 뭉개짐

---

### 3) GeoJSON 변환

```bash
-o format=geojson precision=0.000001
```

- precision: 좌표 소수점 정리

---

### 4) Node 후처리

Node에서 처리하는 작업:

- admCd 생성
- shortAdmCd 생성
- parentAdmCd 생성
- 레벨 설정
- 파일 분할

---

## 9. 결과 구조

```
geo/
  sido.geo.json
  sigungu/
    11.geo.json
    41.geo.json
  emd/
    11110.geo.json
    41131.geo.json
```

---

## 10. 행정코드 규칙

| 레벨 | 예시 | 설명 |
|------|------|------|
| 시도 | 11 | 앞 2자리 |
| 시군구 | 11110 | 앞 5자리 |
| 읍면동 | 1111010100 | 전체 |

---

## 11. 프론트 사용 방식

```javascript
// 시도
/geo/sido.geo.json

// 시군구
/geo/sigungu/{2자리}.geo.json

// 읍면동
/geo/emd/{5자리}.geo.json
```

---

## 12. 성능 최적화 전략

### 필수
- 전체 읍면동 한 번에 로드 금지
- 줌 레벨별 분리 로딩

### 권장
- GeoJSON 파일 캐싱
- gzip 압축 활성화
- properties 최소화

---

## 13. 흔한 문제

### 1) substring 에러
👉 mapshaper에서 문자열 함수 제한
→ Node에서 처리

### 2) .dbf 없음
👉 속성 데이터 없음
→ 반드시 파일 세트 확인

### 3) 좌표 안 맞음
👉 .prj 없음
→ 좌표계 문제

---

## 14. 권장 개발 흐름

```
1. shp 다운로드
2. Node 스크립트 실행
3. static/geo 생성
4. 지도에서 로드
```

---

## 15. 한 줄 요약

👉 "shp를 그대로 쓰지 말고, GeoJSON으로 변환 + 경량화 + 분할 후 사용"

