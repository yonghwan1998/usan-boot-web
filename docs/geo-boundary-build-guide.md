# 행정경계 GeoJSON 구축 가이드 (VWorld 기반)

## 1. 개요

VWorld에서 제공하는 행정경계 SHP 데이터를 GeoJSON으로 변환하고,
Spring Boot static 리소스로 서빙하거나 DB에 적재하는 전체 흐름을 정리한다.

### 전체 흐름

```
VWorld SHP 다운로드
    → build-geo.js 실행 (mapshaper 변환 + Node 후처리)
        → static/geo/*.geo.json 생성
            ├── 프론트: 지도 경계 렌더링 (줌 레벨 기반 lazy load)
            └── 백엔드: POST /map/api/admin/boundaries/seed/all → DB 적재
```

---

## 2. 데이터 다운로드

아래 링크에서 행정경계 데이터를 다운로드한다.

- 시도: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30253
- 시군구: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30252
- 읍면동: https://www.vworld.kr/dtmk/dtmk_ntads_s002.do?dsId=30254

### 주의사항

- 반드시 **전체 압축파일(zip)** 다운로드
- 압축 해제 시 아래 파일들이 모두 존재해야 함

```
.shp  (geometry)
.shx  (index)
.dbf  (속성 데이터)
.prj  (좌표계 정보)
.cpg  (인코딩 정보, 선택)
```

> `.dbf`가 없으면 행정코드/이름을 사용할 수 없음

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
    sido.geo.json
    sigungu/
      {시도코드2자리}.geo.json
    emd/
      {시군구코드5자리}.geo.json
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

## 6. 빌드 실행

```bash
npm run geo:build
```

내부적으로 `node scripts/build-geo.js`를 실행한다.

### 처리 단계

#### 1) 좌표계 변환

```bash
-proj wgs84
```

Naver Map은 WGS84 좌표계 사용.

#### 2) 좌표 단순화

```bash
-simplify {percent} keep-shapes
```

| 레벨 | 값 | 비고 |
|------|----|------|
| 시도 | 8% | |
| 시군구 | 8% | |
| 읍면동 | 5% | 면적이 작아 좀 더 경량화 |

값이 낮을수록 더 가볍지만 경계가 뭉개짐.

#### 3) 불필요 필드 제거 및 GeoJSON 변환

```bash
-filter-fields BJCD,NAME
-o format=geojson precision=0.000001
```

#### 4) Node 후처리 (build-geo.js)

mapshaper 출력 GeoJSON을 읽어 properties를 정규화하고 파일을 분할한다.

| 작업 | 설명 |
|------|------|
| `admCd` 생성 | BJCD 값 그대로 |
| `shortAdmCd` 생성 | 시도: 앞 2자리, 시군구: 앞 5자리, 읍면동: 전체 admCd |
| `parentAdmCd` 생성 | 시군구: 앞 2자리(시도코드), 읍면동: 앞 5자리(시군구코드) |
| `level` 설정 | `SIDO` / `SIGUNGU` / `EMD` 고정값 |
| 파일 분할 | 시군구→시도코드별, 읍면동→시군구코드별 |

---

## 7. GeoJSON properties 명세

### 시도 (`sido.geo.json`)

| 필드 | 타입 | 예시 | 설명 |
|------|------|------|------|
| `admCd` | string | `"1100000000"` | 원본 BJCD (10자리) |
| `shortAdmCd` | string | `"11"` | 앞 2자리 — 파일 분할 기준 |
| `name` | string | `"서울특별시"` | |
| `level` | string | `"SIDO"` | 고정값 |

### 시군구 (`sigungu/{시도코드}.geo.json`)

| 필드 | 타입 | 예시 | 설명 |
|------|------|------|------|
| `admCd` | string | `"1111000000"` | 원본 BJCD (10자리) |
| `shortAdmCd` | string | `"11110"` | 앞 5자리 |
| `parentAdmCd` | string | `"11"` | 시도코드 (앞 2자리) |
| `name` | string | `"종로구"` | |
| `level` | string | `"SIGUNGU"` | 고정값 |

### 읍면동 (`emd/{시군구코드}.geo.json`)

| 필드 | 타입 | 예시 | 설명 |
|------|------|------|------|
| `admCd` | string | `"1111010100"` | 원본 BJCD (10자리) |
| `shortAdmCd` | string | `"1111010100"` | EMD는 admCd와 동일 |
| `parentAdmCd` | string | `"11110"` | 시군구코드 (앞 5자리) |
| `name` | string | `"청운효자동"` | |
| `level` | string | `"EMD"` | 고정값 |

> **주의**: GeoJSON의 `level` 필드는 프론트에서 `feature.getProperty('level')`로 읽는다.
> DB 컬럼명은 `adm_level`이지만 GeoJSON 파일 내 key는 `level`을 그대로 유지한다.
> DB 적재 시 `AdministrativeBoundarySeedService`가 GeoJSON의 `level` 값을 읽어 `adm_level` 컬럼에 저장한다.

---

## 8. DB 테이블 스키마

```sql
CREATE TABLE `administrative_boundary` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `adm_cd`         varchar(10)  NOT NULL                COMMENT '행정구역 코드 (법정동 코드 기반, 10자리)',
  `name`           varchar(100) NOT NULL                COMMENT '행정구역 이름',
  `adm_level`      varchar(20)  NOT NULL                COMMENT '행정구역 레벨 (SIDO, SIGUNGU, EMD)',
  `parent_adm_cd`  varchar(10)  DEFAULT NULL            COMMENT '상위 행정구역 코드 (시군구→시도, 읍면동→시군구)',
  `geom`           geometry     NOT NULL /*!80003 SRID 4326 */ COMMENT '경계 Geometry (WGS84)',
  `created_at`     datetime     NOT NULL                COMMENT '생성일시',
  `updated_at`     datetime     NOT NULL                COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `adm_cd` (`adm_cd`),
  SPATIAL KEY `idx_administrative_boundary_geom` (`geom`),
  KEY `idx_administrative_boundary_adm_level` (`adm_level`),
  KEY `idx_administrative_boundary_parent_adm_cd` (`parent_adm_cd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 9. Spring Boot 연동

### 9-1. GeoJSON 정적 서빙

`static/geo/` 하위 파일들은 Spring Boot가 자동으로 서빙한다.

```
GET /geo/sido.geo.json
GET /geo/sigungu/{시도코드}.geo.json
GET /geo/emd/{시군구코드}.geo.json
```

### 9-2. DB 적재 API

GeoJSON 파일을 DB(`administrative_boundary`)에 적재한다.

```
POST /map/api/admin/boundaries/seed/all
```

- 기존 전체 데이터를 삭제 후 재적재 (TRUNCATE 아닌 DELETE)
- 내부적으로 `UPSERT (ON DUPLICATE KEY UPDATE)` 사용

응답 예시:

```json
{
  "success": true,
  "message": "전국 행정경계 적재가 완료되었습니다.",
  "deletedCount": 5000,
  "sidoCount": 17,
  "sigunguFileCount": 17,
  "sigunguCount": 250,
  "emdFileCount": 250,
  "emdCount": 3500
}
```

### 9-3. 관련 클래스

| 클래스 | 역할 |
|--------|------|
| `AdministrativeBoundary` | 행정구역 JPA 엔티티 |
| `AdministrativeBoundaryRepository` | `findBoundaryContaining(admLevel, lat, lng)` — 좌표 포함 경계 조회 |
| `AdministrativeBoundaryService` | 위경도로 시도/시군구/읍면동 코드 일괄 조회 |
| `AdministrativeBoundarySeedService` | GeoJSON 파일 → DB 적재 |
| `BoundaryCodeResponse` | 시도/시군구/읍면동 코드+이름 응답 DTO |

### 9-4. 행정구역 코드 조회 API

지도 중심 좌표로 해당 위치의 행정구역 코드를 조회한다.

```
GET /map/api/region-code?lat={위도}&lng={경도}
```

응답 예시:

```json
{
  "sidoCode": "41",
  "sigunguCode": "41131",
  "emdCode": "4113110200",
  "sidoName": "경기도",
  "sigunguName": "광주시",
  "emdName": "송정동"
}
```

---

## 10. 행정코드 규칙

| 레벨 | admCd 예시 | sidoCode | sigunguCode | 비고 |
|------|------------|----------|-------------|------|
| 시도 | `1100000000` | `11` | — | 앞 2자리 |
| 시군구 | `1111000000` | `11` | `11110` | 앞 5자리 |
| 읍면동 | `1111010100` | `11` | `11110` | 전체 10자리 |

---

## 11. 프론트 경계 로딩 전략

줌 레벨에 따라 표시할 경계 레벨을 결정하고, 필요한 파일만 lazy load한다.

```
줌 8~10  → 시도 경계   → /geo/sido.geo.json (단일 파일)
줌 11~12 → 시군구 경계 → /geo/sigungu/{sidoCode}.geo.json
줌 13~14 → 읍면동 경계 → /geo/emd/{sigunguCode}.geo.json
줌 15 이상 → 경계 없음
```

시군구/읍면동은 지도 중심 좌표를 `/map/api/region-code`로 조회하여 필요한 파일 경로를 결정한다.

**캐싱**: 동일 URL로 재요청 시 메모리 캐시(`boundaryCache: Map`)에서 즉시 반환한다.

---

## 12. 성능 최적화

### 필수

- 전체 읍면동 한 번에 로드 금지 → 시군구코드별 분할 파일 사용
- 줌 레벨 범위 밖에서는 경계 즉시 제거

### 권장

- GeoJSON 파일 캐싱 (브라우저 캐시 + 서버 gzip 압축)
- properties 최소화 (build-geo.js에서 필요한 필드만 포함)
- DB spatial index (`idx_administrative_boundary_geom`) 활용

---

## 13. 흔한 문제

### 1) mapshaper substring 에러

mapshaper 내에서 문자열 슬라이싱 미지원.
→ Node 후처리에서 `admCd.slice()`로 처리한다.

### 2) .dbf 없음

속성 데이터(BJCD, NAME) 없음.
→ VWorld에서 반드시 파일 세트 전체(shp+shx+dbf+prj)를 다운로드한다.

### 3) 좌표 안 맞음

.prj 파일 없어 좌표계 인식 실패.
→ mapshaper `-proj wgs84` 옵션으로 명시적 변환한다.

### 4) DB 적재 후 ST_Contains 결과 없음

`geom` 컬럼의 SRID가 4326이 아닌 경우.
→ INSERT 시 `ST_SRID(ST_GeomFromGeoJSON(?), 4326)` 사용 확인.
→ `SELECT ST_SRID(geom) FROM administrative_boundary LIMIT 1;` 로 검증.

---

## 14. 개발 흐름

```
1. VWorld에서 시도/시군구/읍면동 SHP 다운로드 후 data/shp/ 에 위치
2. npm run geo:build 실행 → src/main/resources/static/geo/ 에 GeoJSON 생성
3. Spring Boot 실행
4. POST /map/api/admin/boundaries/seed/all 호출 → DB 적재
5. 지도 페이지에서 줌 레벨별 경계 표시 확인
```
