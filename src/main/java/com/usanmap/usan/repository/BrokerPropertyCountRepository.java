package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.usanmap.usan.entity.BrokerPropertyCount;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrokerPropertyCountRepository extends JpaRepository<BrokerPropertyCount, UUID> {

    @Query(value = """
            SELECT COUNT(*) FROM broker_property_count
            WHERE lat BETWEEN :south AND :north
              AND lng BETWEEN :west  AND :east
              AND ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= 1000
            """, nativeQuery = true)
    long countWithin1km(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("south") double south,
            @Param("north") double north,
            @Param("west") double west,
            @Param("east") double east
    );

    List<BrokerPropertyCount> findByLatBetweenAndLngBetween(
            BigDecimal south, BigDecimal north,
            BigDecimal west, BigDecimal east);
    List<BrokerPropertyCount> findByBrokerCodeIn(Collection<UUID> brokerCodes);

    List<BrokerPropertyCount> findBySidoAndSigunguAndEmd(String sido, String sigungu, String emd);

    Optional<BrokerPropertyCount> findByPublicId(String publicId);
}
