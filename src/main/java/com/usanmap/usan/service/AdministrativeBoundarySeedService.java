package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.dto.BoundarySeedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Iterator;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdministrativeBoundarySeedService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public BoundarySeedResponse seedAll() {
        log.info("[BOUNDARY-SEED] 전국 행정경계 적재 시작");

        int deletedCount = deleteAll();
        int sidoCount = importSido();
        FileSeedResult sigunguResult = importGeoJsonFiles("classpath:/static/geo/sigungu/*.geo.json");
        FileSeedResult emdResult = importGeoJsonFiles("classpath:/static/geo/emd/*.geo.json");

        log.info("[BOUNDARY-SEED] 전국 행정경계 적재 완료");

        return BoundarySeedResponse.builder()
                .success(true)
                .message("전국 행정경계 적재가 완료되었습니다.")
                .deletedCount(deletedCount)
                .sidoCount(sidoCount)
                .sigunguFileCount(sigunguResult.fileCount())
                .sigunguCount(sigunguResult.featureCount())
                .emdFileCount(emdResult.fileCount())
                .emdCount(emdResult.featureCount())
                .build();
    }

    private int deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE administrative_boundary");
        log.info("[BOUNDARY-SEED] 기존 전체 데이터 TRUNCATE 완료");
        return 0;
    }

    private int importSido() {
        try {
            Resource resource = resourceResolver.getResource("classpath:/static/geo/sido.geo.json");

            int count = 0;

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode features = root.path("features");

                for (JsonNode feature : features) {
                    upsertFeature(feature);
                    count++;
                }
            }

            log.info("[BOUNDARY-SEED] 시도 적재 완료 - count={}", count);
            return count;
        } catch (Exception e) {
            throw new RuntimeException("시도 데이터 적재 실패", e);
        }
    }

    private FileSeedResult importGeoJsonFiles(String locationPattern) {
        try {
            Resource[] resources = resourceResolver.getResources(locationPattern);

            int fileCount = 0;
            int featureCount = 0;

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    JsonNode features = root.path("features");

                    Iterator<JsonNode> iterator = features.elements();
                    while (iterator.hasNext()) {
                        JsonNode feature = iterator.next();
                        upsertFeature(feature);
                        featureCount++;
                    }

                    fileCount++;
                }
            }

            log.info("[BOUNDARY-SEED] 파일 적재 완료 - pattern={}, files={}, features={}",
                    locationPattern, fileCount, featureCount);

            return new FileSeedResult(fileCount, featureCount);
        } catch (Exception e) {
            throw new RuntimeException("GeoJSON 파일 적재 실패: " + locationPattern, e);
        }
    }

    private void upsertFeature(JsonNode feature) {
        try {
            JsonNode properties = feature.path("properties");

            String admCd = getText(properties, "admCd");
            String name = getText(properties, "name");
            String level = getText(properties, "level");
            String parentAdmCd = getNullableText(properties, "parentAdmCd");

            String geometryJson = objectMapper.writeValueAsString(feature.path("geometry"));

            jdbcTemplate.update("""
                INSERT INTO administrative_boundary
                    (adm_cd, name, adm_level, parent_adm_cd, geom, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ST_SRID(ST_GeomFromGeoJSON(?), 4326), NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    adm_level = VALUES(adm_level),
                    parent_adm_cd = VALUES(parent_adm_cd),
                    geom = VALUES(geom),
                    updated_at = NOW()
                """,
                    admCd,
                    name,
                    level,
                    parentAdmCd,
                    geometryJson
            );
        } catch (Exception e) {
            throw new RuntimeException("feature 적재 실패", e);
        }
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("필수 필드 누락: " + fieldName);
        }
        return value.asText();
    }

    private String getNullableText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private record FileSeedResult(int fileCount, int featureCount) {
    }
}