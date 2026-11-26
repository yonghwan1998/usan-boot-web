package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.usanmap.usan.entity.CrawledListing;

import java.util.List;
import java.util.UUID;

public interface CrawledListingRepository extends JpaRepository<CrawledListing, UUID> {

    boolean existsByCode(UUID code);

    // 중복 시 경계가 있던 시기에 저장된 구 코드를 읍면동의 실제 부모 코드로 복구한다.
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE listing_info
        SET sigungu_code = LEFT(emd_code, 5)
        WHERE CHAR_LENGTH(emd_code) = 10
          AND (sigungu_code IS NULL OR sigungu_code <> LEFT(emd_code, 5))
          AND EXISTS (
              SELECT 1 FROM administrative_boundary emd
              WHERE emd.adm_level = 'EMD'
                AND emd.adm_cd = listing_info.emd_code
                AND emd.parent_adm_cd = LEFT(listing_info.emd_code, 5)
          )
        """, nativeQuery = true)
    int correctSigunguCodesFromEmd();

    @Query("SELECT c.listingType as listingType, COUNT(c) as count FROM CrawledListing c WHERE c.sidoCode = :code GROUP BY c.listingType")
    List<ListingTypeStat> countByListingTypeAndSidoCode(@Param("code") String code);

    @Query("SELECT c.listingType as listingType, COUNT(c) as count FROM CrawledListing c WHERE c.sigunguCode = :code GROUP BY c.listingType")
    List<ListingTypeStat> countByListingTypeAndSigunguCode(@Param("code") String code);

    @Query("SELECT c.listingType as listingType, COUNT(c) as count FROM CrawledListing c WHERE c.emdCode = :code GROUP BY c.listingType")
    List<ListingTypeStat> countByListingTypeAndEmdCode(@Param("code") String code);

    @Query("SELECT COUNT(DISTINCT c.crawledBroker.brokerCode) FROM CrawledListing c WHERE c.sidoCode = :code")
    long countDistinctBrokerBySidoCode(@Param("code") String code);

    @Query("SELECT COUNT(DISTINCT c.crawledBroker.brokerCode) FROM CrawledListing c WHERE c.sigunguCode = :code")
    long countDistinctBrokerBySigunguCode(@Param("code") String code);

    @Query("SELECT COUNT(DISTINCT c.crawledBroker.brokerCode) FROM CrawledListing c WHERE c.emdCode = :code")
    long countDistinctBrokerByEmdCode(@Param("code") String code);

    interface ListingTypeStat {
        String getListingType();
        long getCount();
    }
}
