package world.usan.usan.batch.job.brokerproperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebuildBrokerPropertyCountTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        //1. 기존 데이터 삭제
        int deleted = jdbcTemplate.update("DELETE FROM broker_property_count");
        log.info("[broker_property_count] deleted rows={}", deleted);

        //2. 새 데이터 INSERT, broker_info + listing_info 집계
        String sql = """
                INSERT INTO broker_property_count (
                      broker_code
                    , broker_name
                    , office_name
                    , registration_number
                    , tel
                    , phone
                    , sido
                    , sigungu
                    , emd
                    , road_name
                    , addr_road
                    , addr_jibun
                    , lat
                    , lng
                    , apt_cnt
                    , officetel_cnt
                    , villa_cnt
                    , oneroom_cnt
                    , tworoom_cnt
                    , detached_cnt
                    , rural_cnt
                    , mixedhouse_cnt
                    , hanok_cnt
                    , store_cnt
                    , office_cnt
                    , building_cnt
                    , factory_cnt
                    , knowledge_cnt
                    , land_cnt
                    , apt_sale_cnt
                    , officetel_sale_cnt
                    , redevelopment_cnt
                    , reconstruction_cnt
                    , presale_cnt
                    , created_at
                    , updated_at
                    , deleted_at
                )
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
                    , b.addr_road
                    , b.addr_jibun
                    , b.lat
                    , b.lng
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
                    , NOW() AS created_at
                    , NOW() AS updated_at
                    , NULL AS deleted_at
                FROM broker_info b
                LEFT JOIN listing_info l ON l.broker_code = b.broker_code
                GROUP BY b.broker_code
                """;

        int inserted = jdbcTemplate.update(sql);
        log.info("[broker_property_count] inserted rows={}", inserted);

        return RepeatStatus.FINISHED;
    }
}
