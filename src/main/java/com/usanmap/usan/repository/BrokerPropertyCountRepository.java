package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.usanmap.usan.dto.BrokerMarkerDto;
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

    @Query(value = """
            SELECT
              SUM(ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= 500),
              SUM(ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= 1000),
              SUM(ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= 2000),
              SUM(ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= 3000)
            FROM broker_property_count
            WHERE lat BETWEEN :south AND :north
              AND lng BETWEEN :west  AND :east
            """, nativeQuery = true)
    List<Object[]> countByRadii(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("south") double south,
            @Param("north") double north,
            @Param("west") double west,
            @Param("east") double east
    );

    @Query(value = """
            SELECT * FROM broker_property_count
            WHERE lat BETWEEN :south AND :north
              AND lng BETWEEN :west  AND :east
              AND ST_Distance_Sphere(POINT(lng, lat), POINT(:lng, :lat)) <= :radiusM
            """, nativeQuery = true)
    List<BrokerPropertyCount> findInRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusM") double radiusM,
            @Param("south") double south,
            @Param("north") double north,
            @Param("west") double west,
            @Param("east") double east
    );

    @Query("SELECT new com.usanmap.usan.dto.BrokerMarkerDto(b.brokerCode, b.brokerName, b.officeName, b.lat, b.lng) " +
           "FROM BrokerPropertyCount b " +
           "WHERE b.lat BETWEEN :south AND :north AND b.lng BETWEEN :west AND :east")
    List<BrokerMarkerDto> findMarkersInBounds(
            @Param("south") BigDecimal south, @Param("north") BigDecimal north,
            @Param("west") BigDecimal west, @Param("east") BigDecimal east);
    List<BrokerPropertyCount> findByBrokerCodeIn(Collection<UUID> brokerCodes);

    List<BrokerPropertyCount> findBySidoAndSigunguAndEmd(String sido, String sigungu, String emd);

    Optional<BrokerPropertyCount> findByPublicId(String publicId);
}
