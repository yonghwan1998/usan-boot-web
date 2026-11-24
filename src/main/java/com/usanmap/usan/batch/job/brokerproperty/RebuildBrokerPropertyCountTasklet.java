package com.usanmap.usan.batch.job.brokerproperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import com.usanmap.usan.util.PublicIdUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebuildBrokerPropertyCountTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    static final int PAGE_SIZE = 1000;
    private static final String LOCK_NAME = "usan.broker-property-count";
    private static final String STAGE = "broker_property_count_stage";

    private static final String SELECT_SQL = """
            SELECT
                  b.broker_code
                , b.broker_name
                , b.office_name
                , b.registration_number
                , b.tel
                , b.phone
                , b.sido
                , b.sigungu
                , b.emd
                , b.road_name
                , b.road_address
                , b.jibun_address
                , b.lat
                , b.lng
                , (SELECT p.public_id FROM broker_property_count p
                   WHERE p.broker_code = b.broker_code) AS public_id
                , SUM(CASE WHEN l.listing_type = '아파트'        THEN 1 ELSE 0 END) AS apt_cnt
                , SUM(CASE WHEN l.listing_type = '오피스텔'      THEN 1 ELSE 0 END) AS officetel_cnt
                , SUM(CASE WHEN l.listing_type = '빌라/연립'     THEN 1 ELSE 0 END) AS villa_cnt
                , SUM(CASE WHEN l.listing_type = '원룸'          THEN 1 ELSE 0 END) AS oneroom_cnt
                , SUM(CASE WHEN l.listing_type = '투룸'          THEN 1 ELSE 0 END) AS tworoom_cnt
                , SUM(CASE WHEN l.listing_type = '단독/다가구'   THEN 1 ELSE 0 END) AS detached_cnt
                , SUM(CASE WHEN l.listing_type = '전원주택'      THEN 1 ELSE 0 END) AS rural_cnt
                , SUM(CASE WHEN l.listing_type = '상가주택'      THEN 1 ELSE 0 END) AS mixedhouse_cnt
                , SUM(CASE WHEN l.listing_type = '한옥주택'      THEN 1 ELSE 0 END) AS hanok_cnt
                , SUM(CASE WHEN l.listing_type = '상가'          THEN 1 ELSE 0 END) AS store_cnt
                , SUM(CASE WHEN l.listing_type = '사무실'        THEN 1 ELSE 0 END) AS office_cnt
                , SUM(CASE WHEN l.listing_type = '건물'          THEN 1 ELSE 0 END) AS building_cnt
                , SUM(CASE WHEN l.listing_type = '공장/창고'      THEN 1 ELSE 0 END) AS factory_cnt
                , SUM(CASE WHEN l.listing_type = '지식산업센터'  THEN 1 ELSE 0 END) AS knowledge_cnt
                , SUM(CASE WHEN l.listing_type = '토지'          THEN 1 ELSE 0 END) AS land_cnt
                , SUM(CASE WHEN l.listing_type = '아파트분양권'  THEN 1 ELSE 0 END) AS apt_sale_cnt
                , SUM(CASE WHEN l.listing_type = '오피스텔분양권' THEN 1 ELSE 0 END) AS officetel_sale_cnt
                , SUM(CASE WHEN l.listing_type = '재개발'        THEN 1 ELSE 0 END) AS redevelopment_cnt
                , SUM(CASE WHEN l.listing_type = '재건축'        THEN 1 ELSE 0 END) AS reconstruction_cnt
                , SUM(CASE WHEN l.listing_type = '분양중/예정'    THEN 1 ELSE 0 END) AS presale_cnt
            FROM broker_info b
            LEFT JOIN listing_info l ON l.broker_code = b.broker_code
            WHERE b.broker_code IN (%s)
            GROUP BY b.broker_code
            ORDER BY b.broker_code
            """;

    private static final String INSERT_SQL = """
            INSERT INTO broker_property_count_stage (
                  broker_code, public_id, broker_name, office_name
                , registration_number, tel, phone
                , sido, sigungu, emd, road_name, addr_road, addr_jibun
                , lat, lng
                , apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt
                , detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt
                , store_cnt, office_cnt, building_cnt, factory_cnt
                , knowledge_cnt, land_cnt, apt_sale_cnt, officetel_sale_cnt
                , redevelopment_cnt, reconstruction_cnt, presale_cnt
                , created_at, updated_at, deleted_at
            ) VALUES (
                  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                , NOW(), NOW(), NULL
            )
            """;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        return jdbcTemplate.execute((ConnectionCallback<RepeatStatus>) connection -> {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            Integer locked = jdbc.queryForObject("SELECT GET_LOCK(?, 0)", Integer.class, LOCK_NAME);
            if (!Integer.valueOf(1).equals(locked)) {
                throw new IllegalStateException("broker_property_count batch already running");
            }
            try {
                String engine = jdbc.queryForObject("""
                        SELECT ENGINE FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'broker_property_count'
                        """, String.class);
                if (!"InnoDB".equalsIgnoreCase(engine)) {
                    throw new IllegalStateException("broker_property_count must use InnoDB for atomic publication");
                }
                // Temporary tables and the advisory lock belong to this physical connection.
                jdbc.execute("CREATE TEMPORARY TABLE " + STAGE + " LIKE broker_property_count");
                rebuild(jdbc, connection);
                return RepeatStatus.FINISHED;
            } finally {
                try {
                    jdbc.execute("DROP TEMPORARY TABLE IF EXISTS " + STAGE);
                } finally {
                    jdbc.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, LOCK_NAME);
                }
            }
        });
    }

    void rebuild(JdbcTemplate jdbc, Connection connection) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            byte[] lastCode = null;
            long staged = 0;
            while (true) {
                List<byte[]> codes = lastCode == null
                        ? jdbc.query("SELECT broker_code FROM broker_info ORDER BY broker_code LIMIT ?",
                                (rs, rowNum) -> rs.getBytes(1), PAGE_SIZE)
                        : jdbc.query("SELECT broker_code FROM broker_info WHERE broker_code > ? "
                                        + "ORDER BY broker_code LIMIT ?",
                                (rs, rowNum) -> rs.getBytes(1), lastCode, PAGE_SIZE);
                if (codes.isEmpty()) break;

                String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
                List<Map<String, Object>> rows = jdbc.queryForList(
                        SELECT_SQL.formatted(placeholders), codes.toArray());
                if (rows.size() != codes.size()) {
                    throw new IllegalStateException("Broker source changed during aggregation; retry batch");
                }
                stagePage(jdbc, connection, rows);
                staged += rows.size();
                lastCode = codes.get(codes.size() - 1);
                log.info("[broker_property_count] staged rows={}", staged);
            }
            // Do not replace a populated table with an unexpectedly empty source.
            if (staged == 0) throw new IllegalStateException("No brokers to aggregate");
            Long actual = jdbc.queryForObject("SELECT COUNT(*) FROM " + STAGE, Long.class);
            if (actual == null || actual != staged) {
                throw new IllegalStateException("Staging row count mismatch");
            }
            jdbc.update("DELETE FROM broker_property_count");
            int inserted = jdbc.update(PUBLISH_SQL);
            if (inserted != staged) throw new IllegalStateException("Published row count mismatch");
            connection.commit();
            log.info("[broker_property_count] published rows={}", inserted);
        } catch (SQLException | RuntimeException | Error e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void stagePage(JdbcTemplate jdbc, Connection connection,
                           List<Map<String, Object>> rows) throws SQLException {
        List<Map<String, Object>> newBrokers = rows.stream()
                .filter(row -> row.get("public_id") == null).toList();
        for (int attempt = 0; attempt < 5; attempt++) {
            for (Map<String, Object> row : newBrokers) {
                String publicId;
                do {
                    publicId = PublicIdUtils.generate();
                } while (Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM broker_property_count WHERE public_id = ?)",
                        Boolean.class, publicId)));
                row.put("public_id", publicId);
            }
            List<Object[]> args = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                args.add(new Object[] {
                        row.get("broker_code"),
                        row.get("public_id"),
                        row.get("broker_name"),
                        row.get("office_name"),
                        row.get("registration_number"),
                        row.get("tel"),
                        row.get("phone"),
                        row.get("sido"),
                        row.get("sigungu"),
                        row.get("emd"),
                        row.get("road_name"),
                        row.get("road_address"),
                        row.get("jibun_address"),
                        row.get("lat"),
                        row.get("lng"),
                        row.get("apt_cnt"),
                        row.get("officetel_cnt"),
                        row.get("villa_cnt"),
                        row.get("oneroom_cnt"),
                        row.get("tworoom_cnt"),
                        row.get("detached_cnt"),
                        row.get("rural_cnt"),
                        row.get("mixedhouse_cnt"),
                        row.get("hanok_cnt"),
                        row.get("store_cnt"),
                        row.get("office_cnt"),
                        row.get("building_cnt"),
                        row.get("factory_cnt"),
                        row.get("knowledge_cnt"),
                        row.get("land_cnt"),
                        row.get("apt_sale_cnt"),
                        row.get("officetel_sale_cnt"),
                        row.get("redevelopment_cnt"),
                        row.get("reconstruction_cnt"),
                        row.get("presale_cnt")
                });
            }
            try {
                jdbc.batchUpdate(INSERT_SQL, args);
                connection.commit();
                return;
            } catch (DuplicateKeyException e) {
                connection.rollback();
                if (attempt == 4) throw e;
            }
        }
    }

    private static final String PUBLISH_SQL = """
            INSERT INTO broker_property_count (
                  broker_code, public_id, broker_name, office_name
                , registration_number, tel, phone
                , sido, sigungu, emd, road_name, addr_road, addr_jibun
                , lat, lng
                , apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt
                , detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt
                , store_cnt, office_cnt, building_cnt, factory_cnt
                , knowledge_cnt, land_cnt, apt_sale_cnt, officetel_sale_cnt
                , redevelopment_cnt, reconstruction_cnt, presale_cnt
                , created_at, updated_at, deleted_at
            ) SELECT
                  broker_code, public_id, broker_name, office_name
                , registration_number, tel, phone
                , sido, sigungu, emd, road_name, addr_road, addr_jibun
                , lat, lng
                , apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt
                , detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt
                , store_cnt, office_cnt, building_cnt, factory_cnt
                , knowledge_cnt, land_cnt, apt_sale_cnt, officetel_sale_cnt
                , redevelopment_cnt, reconstruction_cnt, presale_cnt
                , created_at, updated_at, deleted_at
            FROM broker_property_count_stage
            """;
}
