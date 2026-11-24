package com.usanmap.usan.batch.job.brokerproperty;

import com.usanmap.usan.util.PublicIdUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class RebuildBrokerPropertyCountTaskletTests {
    private Connection connection;
    private JdbcTemplate jdbc;
    private RebuildBrokerPropertyCountTasklet tasklet;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        tasklet = new RebuildBrokerPropertyCountTasklet(jdbc);
        jdbc.execute("""
                CREATE TABLE broker_info (
                    broker_code BINARY(16) PRIMARY KEY, broker_name VARCHAR(100), office_name VARCHAR(100),
                    registration_number VARCHAR(100), tel VARCHAR(100), phone VARCHAR(100),
                    sido VARCHAR(100), sigungu VARCHAR(100), emd VARCHAR(100), road_name VARCHAR(100),
                    road_address VARCHAR(100), jibun_address VARCHAR(100), lat DECIMAL(10,7), lng DECIMAL(10,7))
                """);
        jdbc.execute("CREATE TABLE listing_info (broker_code BINARY(16), listing_type VARCHAR(100))");
        String schema = """
                (broker_code BINARY(16) PRIMARY KEY, public_id VARCHAR(6) NOT NULL UNIQUE,
                 broker_name VARCHAR(100), office_name VARCHAR(100), registration_number VARCHAR(100),
                 tel VARCHAR(100), phone VARCHAR(100), sido VARCHAR(100), sigungu VARCHAR(100), emd VARCHAR(100),
                 road_name VARCHAR(100), addr_road VARCHAR(100), addr_jibun VARCHAR(100),
                 lat DECIMAL(10,7), lng DECIMAL(10,7),
                 apt_cnt INT, officetel_cnt INT, villa_cnt INT, oneroom_cnt INT, tworoom_cnt INT,
                 detached_cnt INT, rural_cnt INT, mixedhouse_cnt INT, hanok_cnt INT,
                 store_cnt INT, office_cnt INT, building_cnt INT, factory_cnt INT,
                 knowledge_cnt INT, land_cnt INT, apt_sale_cnt INT, officetel_sale_cnt INT,
                 redevelopment_cnt INT, reconstruction_cnt INT, presale_cnt INT,
                 created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP,
                 total_cnt INT GENERATED ALWAYS AS (apt_cnt + officetel_cnt))
                """;
        jdbc.execute("CREATE TABLE broker_property_count " + schema);
        jdbc.execute("CREATE TABLE broker_property_count_stage " + schema);
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    @Test
    void aggregatesTypesIncludingZeroAndKeepsExistingShareId() throws Exception {
        broker(1); broker(2);
        jdbc.update("INSERT INTO broker_property_count (broker_code, public_id) VALUES (?, 'keep01')", code(1));
        for (String type : List.of("아파트", "아파트", "오피스텔", "토지", "unknown")) {
            jdbc.update("INSERT INTO listing_info VALUES (?, ?)", code(1), type);
        }
        // Orphan listings must not create an extra broker.
        jdbc.update("INSERT INTO listing_info VALUES (?, '아파트')", code(999));
        tasklet.rebuild(jdbc, connection);
        var first = jdbc.queryForMap("SELECT * FROM broker_property_count WHERE broker_code = ?", code(1));
        assertThat(first.get("public_id")).isEqualTo("keep01");
        assertThat(first.get("apt_cnt")).isEqualTo(2);
        assertThat(first.get("officetel_cnt")).isEqualTo(1);
        assertThat(first.get("land_cnt")).isEqualTo(1);
        assertThat(first.get("total_cnt")).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT apt_cnt FROM broker_property_count WHERE broker_code = ?",
                Integer.class, code(2))).isZero();
        String newId = jdbc.queryForObject("SELECT public_id FROM broker_property_count WHERE broker_code = ?",
                String.class, code(2));
        assertThat(newId).matches("[a-z0-9]{6}");
        assertThat(connection.getAutoCommit()).isTrue();
    }

    @Test
    void processesMoreThanOnePageWithoutOmittingOrDuplicatingBrokers() throws Exception {
        List<Object[]> brokers = new ArrayList<>();
        for (int i = 1; i <= 1003; i++) brokers.add(new Object[]{code(i)});
        jdbc.batchUpdate("INSERT INTO broker_info (broker_code) VALUES (?)", brokers);
        tasklet.rebuild(jdbc, connection);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM broker_property_count", Integer.class)).isEqualTo(1003);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT public_id) FROM broker_property_count",
                Integer.class)).isEqualTo(1003);
    }

    @Test
    void publicationFailureRollsBackDeleteAndKeepsLiveData() {
        broker(1);
        jdbc.update("INSERT INTO broker_property_count (broker_code, public_id, apt_cnt) VALUES (?, 'keep01', 42)", code(1));
        jdbc.execute("ALTER TABLE broker_property_count ADD CONSTRAINT refuse_new_count CHECK (apt_cnt = 42)");
        assertThatThrownBy(() -> tasklet.rebuild(jdbc, connection)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT apt_cnt FROM broker_property_count WHERE public_id = 'keep01'",
                Integer.class)).isEqualTo(42);
    }

    @Test
    void emptySourceKeepsLiveData() {
        jdbc.update("INSERT INTO broker_property_count (broker_code, public_id) VALUES (?, 'keep01')", code(1));
        assertThatThrownBy(() -> tasklet.rebuild(jdbc, connection)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT public_id FROM broker_property_count", String.class)).isEqualTo("keep01");
    }

    @Test
    void duplicateGeneratedIdsRetryTheWholePageWithoutPartialRows() throws Exception {
        broker(1); broker(2);
        try (var ids = mockStatic(PublicIdUtils.class)) {
            ids.when(PublicIdUtils::generate).thenReturn("same01", "same01", "new001", "new002");
            tasklet.rebuild(jdbc, connection);
        }
        assertThat(jdbc.queryForList("SELECT public_id FROM broker_property_count ORDER BY broker_code", String.class))
                .containsExactly("new001", "new002");
    }

    @Test
    void newBrokerCannotTakeAnExistingBrokersShareId() throws Exception {
        broker(1); broker(2);
        jdbc.update("INSERT INTO broker_property_count (broker_code, public_id) VALUES (?, 'keep01')", code(2));
        try (var ids = mockStatic(PublicIdUtils.class)) {
            ids.when(PublicIdUtils::generate).thenReturn("keep01", "new001");
            tasklet.rebuild(jdbc, connection);
        }
        assertThat(jdbc.queryForList("SELECT public_id FROM broker_property_count ORDER BY broker_code", String.class))
                .containsExactly("new001", "keep01");
    }

    @Test
    void stagingFailureKeepsLiveData() {
        broker(1);
        jdbc.update("INSERT INTO broker_property_count (broker_code, public_id, apt_cnt) VALUES (?, 'keep01', 42)", code(1));
        jdbc.execute("ALTER TABLE broker_property_count_stage ADD CONSTRAINT reject_stage CHECK (apt_cnt > 0)");
        assertThatThrownBy(() -> tasklet.rebuild(jdbc, connection)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT apt_cnt FROM broker_property_count WHERE public_id = 'keep01'",
                Integer.class)).isEqualTo(42);
    }

    private void broker(int id) {
        jdbc.update("INSERT INTO broker_info (broker_code, broker_name) VALUES (?, ?)", code(id), "broker" + id);
    }

    private byte[] code(int id) {
        return ByteBuffer.allocate(16).putLong(0).putLong(id).array();
    }
}
