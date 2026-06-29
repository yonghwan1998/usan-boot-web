package com.usanmap.usan.repository;

import com.usanmap.usan.entity.RegionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RegionStatRepository extends JpaRepository<RegionStat, String> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RegionStat")
    void clearAll();

    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        INSERT INTO region_stat
            (adm_cd, adm_level, broker_count,
             apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt,
             detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt,
             store_cnt, office_cnt, building_cnt, factory_cnt, knowledge_cnt,
             land_cnt, apt_sale_cnt, officetel_sale_cnt,
             redevelopment_cnt, reconstruction_cnt, presale_cnt,
             total_cnt, updated_at)
        SELECT
            sido_code, 'SIDO', COUNT(DISTINCT broker_code),
            SUM(listing_type = '아파트'),
            SUM(listing_type = '오피스텔'),
            SUM(listing_type = '빌라/연립'),
            SUM(listing_type = '원룸'),
            SUM(listing_type = '투룸'),
            SUM(listing_type = '단독/다가구'),
            SUM(listing_type = '전원주택'),
            SUM(listing_type = '상가주택'),
            SUM(listing_type = '한옥주택'),
            SUM(listing_type = '상가'),
            SUM(listing_type = '사무실'),
            SUM(listing_type = '건물'),
            SUM(listing_type = '공장/창고'),
            SUM(listing_type = '지식산업센터'),
            SUM(listing_type = '토지'),
            SUM(listing_type = '아파트분양권'),
            SUM(listing_type = '오피스텔분양권'),
            SUM(listing_type = '재개발'),
            SUM(listing_type = '재건축'),
            SUM(listing_type = '분양중/예정'),
            COUNT(*),
            NOW()
        FROM listing_info
        WHERE sido_code IS NOT NULL
        GROUP BY sido_code
        """)
    void insertSidoStats();

    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        INSERT INTO region_stat
            (adm_cd, adm_level, broker_count,
             apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt,
             detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt,
             store_cnt, office_cnt, building_cnt, factory_cnt, knowledge_cnt,
             land_cnt, apt_sale_cnt, officetel_sale_cnt,
             redevelopment_cnt, reconstruction_cnt, presale_cnt,
             total_cnt, updated_at)
        SELECT
            sigungu_code, 'SIGUNGU', COUNT(DISTINCT broker_code),
            SUM(listing_type = '아파트'),
            SUM(listing_type = '오피스텔'),
            SUM(listing_type = '빌라/연립'),
            SUM(listing_type = '원룸'),
            SUM(listing_type = '투룸'),
            SUM(listing_type = '단독/다가구'),
            SUM(listing_type = '전원주택'),
            SUM(listing_type = '상가주택'),
            SUM(listing_type = '한옥주택'),
            SUM(listing_type = '상가'),
            SUM(listing_type = '사무실'),
            SUM(listing_type = '건물'),
            SUM(listing_type = '공장/창고'),
            SUM(listing_type = '지식산업센터'),
            SUM(listing_type = '토지'),
            SUM(listing_type = '아파트분양권'),
            SUM(listing_type = '오피스텔분양권'),
            SUM(listing_type = '재개발'),
            SUM(listing_type = '재건축'),
            SUM(listing_type = '분양중/예정'),
            COUNT(*),
            NOW()
        FROM listing_info
        WHERE sigungu_code IS NOT NULL
        GROUP BY sigungu_code
        """)
    void insertSigunguStats();

    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
        INSERT INTO region_stat
            (adm_cd, adm_level, broker_count,
             apt_cnt, officetel_cnt, villa_cnt, oneroom_cnt, tworoom_cnt,
             detached_cnt, rural_cnt, mixedhouse_cnt, hanok_cnt,
             store_cnt, office_cnt, building_cnt, factory_cnt, knowledge_cnt,
             land_cnt, apt_sale_cnt, officetel_sale_cnt,
             redevelopment_cnt, reconstruction_cnt, presale_cnt,
             total_cnt, updated_at)
        SELECT
            emd_code, 'EMD', COUNT(DISTINCT broker_code),
            SUM(listing_type = '아파트'),
            SUM(listing_type = '오피스텔'),
            SUM(listing_type = '빌라/연립'),
            SUM(listing_type = '원룸'),
            SUM(listing_type = '투룸'),
            SUM(listing_type = '단독/다가구'),
            SUM(listing_type = '전원주택'),
            SUM(listing_type = '상가주택'),
            SUM(listing_type = '한옥주택'),
            SUM(listing_type = '상가'),
            SUM(listing_type = '사무실'),
            SUM(listing_type = '건물'),
            SUM(listing_type = '공장/창고'),
            SUM(listing_type = '지식산업센터'),
            SUM(listing_type = '토지'),
            SUM(listing_type = '아파트분양권'),
            SUM(listing_type = '오피스텔분양권'),
            SUM(listing_type = '재개발'),
            SUM(listing_type = '재건축'),
            SUM(listing_type = '분양중/예정'),
            COUNT(*),
            NOW()
        FROM listing_info
        WHERE emd_code IS NOT NULL
        GROUP BY emd_code
        """)
    void insertEmdStats();
}
