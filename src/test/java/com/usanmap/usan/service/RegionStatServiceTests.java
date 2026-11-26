package com.usanmap.usan.service;

import com.usanmap.usan.repository.RegionStatRepository;
import com.usanmap.usan.repository.CrawledListingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RegionStatServiceTests {

    private Connection connection;
    private JdbcTemplate jdbc;
    private RegionStatService service;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbc.execute("""
                CREATE TABLE listing_info (
                    broker_code VARCHAR(36), listing_type VARCHAR(50),
                    sido_code VARCHAR(2), sigungu_code VARCHAR(5), emd_code VARCHAR(10))
                """);
        jdbc.execute("""
                CREATE TABLE administrative_boundary (
                    adm_cd VARCHAR(10), adm_level VARCHAR(10), parent_adm_cd VARCHAR(10))
                """);
        jdbc.execute("""
                CREATE TABLE region_stat (
                    adm_cd VARCHAR(10) PRIMARY KEY, adm_level VARCHAR(10), broker_count INT,
                    apt_cnt INT, officetel_cnt INT, villa_cnt INT, oneroom_cnt INT, tworoom_cnt INT,
                    detached_cnt INT, rural_cnt INT, mixedhouse_cnt INT, hanok_cnt INT,
                    store_cnt INT, office_cnt INT, building_cnt INT, factory_cnt INT, knowledge_cnt INT,
                    land_cnt INT, apt_sale_cnt INT, officetel_sale_cnt INT,
                    redevelopment_cnt INT, reconstruction_cnt INT, presale_cnt INT,
                    total_cnt INT, updated_at TIMESTAMP)
                """);
        RegionStatRepository repository = mock(RegionStatRepository.class);
        doAnswer(call -> { jdbc.update("DELETE FROM region_stat"); return null; }).when(repository).clearAll();
        doAnswer(call -> { executeQuery(RegionStatRepository.class, "insertSidoStats"); return null; })
                .when(repository).insertSidoStats();
        doAnswer(call -> { executeQuery(RegionStatRepository.class, "insertSigunguStats"); return null; })
                .when(repository).insertSigunguStats();
        doAnswer(call -> { executeQuery(RegionStatRepository.class, "insertEmdStats"); return null; })
                .when(repository).insertEmdStats();
        CrawledListingRepository listings = mock(CrawledListingRepository.class);
        doAnswer(call -> jdbc.update(CrawledListingRepository.class.getMethod("correctSigunguCodesFromEmd")
                .getAnnotation(Query.class).value())).when(listings).correctSigunguCodesFromEmd();
        service = new RegionStatService(repository, listings);
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    private void executeQuery(Class<?> repository, String method) throws Exception {
        jdbc.update(repository.getMethod(method).getAnnotation(Query.class).value());
    }

    @ParameterizedTest
    @CsvSource({"41110,41113,4111312600", "41130,41135,4113510100", "41170,41171,4117110100"})
    void refreshRestoresDistrictStatsFromListingsPreviouslyAssignedToCity(
            String city, String district, String emd) {
        jdbc.update("INSERT INTO administrative_boundary VALUES (?, 'EMD', ?)", emd, district);
        listing("broker1", "아파트", city, emd);
        listing("broker1", "아파트", city, emd);
        listing("broker2", "원룸", city, emd);
        listing("broker2", "원룸", district, emd);

        service.refresh();

        assertThat(jdbc.queryForObject("SELECT total_cnt FROM region_stat WHERE adm_cd = ?", Integer.class, emd))
                .isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT adm_cd FROM region_stat WHERE adm_level = 'SIGUNGU'", String.class))
                .containsExactly(district);
        assertThat(jdbc.queryForObject("SELECT broker_count FROM region_stat WHERE adm_cd = ?", Integer.class, district))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT apt_cnt FROM region_stat WHERE adm_cd = ?", Integer.class, district))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT oneroom_cnt FROM region_stat WHERE adm_cd = ?", Integer.class, district))
                .isEqualTo(2);

        service.refresh();
        assertThat(jdbc.queryForObject("SELECT total_cnt FROM region_stat WHERE adm_cd = ?", Integer.class, district))
                .isEqualTo(4);
    }

    @Test
    void refreshRestoresMissingDistrictCodeWhenEmdParentIsKnown() {
        jdbc.update("INSERT INTO administrative_boundary VALUES ('4111312600', 'EMD', '41113')");
        listing("broker1", "아파트", null, "4111312600");

        service.refresh();

        assertThat(jdbc.queryForObject("SELECT sigungu_code FROM listing_info", String.class)).isEqualTo("41113");
        assertThat(jdbc.queryForObject("SELECT broker_count FROM region_stat WHERE adm_cd = '41113'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void correctionPreservesCodesWithoutConfirmedEmdParent() throws Exception {
        jdbc.update("INSERT INTO administrative_boundary VALUES ('4111312600', 'EMD', '41110')");
        listing("no-emd", "아파트", "41113", null);
        listing("empty-emd", "아파트", "41113", "");
        listing("short-emd", "아파트", "41113", "41113");
        listing("unknown-emd", "아파트", "41113", "4111319999");
        listing("wrong-parent", "아파트", "41110", "4111312600");

        executeQuery(CrawledListingRepository.class, "correctSigunguCodesFromEmd");

        assertThat(jdbc.queryForList("SELECT sigungu_code FROM listing_info ORDER BY broker_code", String.class))
                .containsExactly("41113", "41113", "41113", "41113", "41110");
    }

    private void listing(String broker, String type, String sigungu, String emd) {
        jdbc.update("INSERT INTO listing_info VALUES (?, ?, '41', ?, ?)", broker, type, sigungu, emd);
    }
}
