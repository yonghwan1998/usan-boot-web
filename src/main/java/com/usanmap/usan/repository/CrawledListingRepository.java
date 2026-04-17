package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.usanmap.usan.entity.CrawledListing;

import java.util.List;
import java.util.UUID;

public interface CrawledListingRepository extends JpaRepository<CrawledListing, UUID> {

    boolean existsByCode(UUID code);

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
