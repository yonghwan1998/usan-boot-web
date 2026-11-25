-- #382: 기존 administrative_boundary 데이터만 보정한다.
-- 수정된 GeoJSON 배포와 함께 실행한다. 신규 DB는 수정된 GeoJSON으로 seed한다.
-- 일반구의 행정코드, 경계, 중심좌표 및 읍면동 데이터는 유지한다.
START TRANSACTION;

UPDATE administrative_boundary district
JOIN administrative_boundary city
  ON LEFT(city.adm_cd, 5) = CONCAT(LEFT(district.adm_cd, 4), '0')
 AND city.adm_level = 'SIGUNGU'
 AND city.name LIKE '%시'
SET district.name = CONCAT(city.name, ' ', district.name),
    district.updated_at = NOW()
WHERE district.adm_level = 'SIGUNGU'
  AND SUBSTRING(district.adm_cd, 5, 1) <> '0'
  AND district.name LIKE '%구'
  AND district.name NOT LIKE CONCAT(city.name, ' %');

DELETE city
FROM administrative_boundary city
JOIN administrative_boundary district
  ON LEFT(city.adm_cd, 5) = CONCAT(LEFT(district.adm_cd, 4), '0')
 AND district.adm_level = 'SIGUNGU'
 AND SUBSTRING(district.adm_cd, 5, 1) <> '0'
 AND district.name LIKE '%구'
WHERE city.adm_level = 'SIGUNGU'
  AND city.name LIKE '%시';

COMMIT;
