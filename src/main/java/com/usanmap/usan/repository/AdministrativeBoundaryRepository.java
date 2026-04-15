package com.usanmap.usan.repository;

import com.usanmap.usan.entity.AdministrativeBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdministrativeBoundaryRepository extends JpaRepository<AdministrativeBoundary, Long> {

    @Query(value = """
        SELECT *
        FROM administrative_boundary ab
        WHERE ab.adm_level = :admLevel
          AND ST_Contains(
                ab.geom,
                ST_SRID(POINT(:lng, :lat), 4326)
          )
        LIMIT 1
        """, nativeQuery = true)
    Optional<AdministrativeBoundary> findBoundaryContaining(
            @Param("admLevel") String admLevel,
            @Param("lat") double lat,
            @Param("lng") double lng
    );

    @Query(value = """
        SELECT ab.name                                                          AS name,
               ST_X(ST_Centroid(ST_GeomFromWKB(ST_AsBinary(ab.geom))))         AS lat,
               ST_Y(ST_Centroid(ST_GeomFromWKB(ST_AsBinary(ab.geom))))         AS lng
        FROM administrative_boundary ab
        WHERE ab.adm_level = :admLevel
          AND MBRIntersects(
                ab.geom,
                ST_GeomFromText(CONCAT(
                  'POLYGON((',
                  :south, ' ', :west,  ',',
                  :south, ' ', :east,  ',',
                  :north, ' ', :east,  ',',
                  :north, ' ', :west,  ',',
                  :south, ' ', :west,
                  '))'
                ), 4326)
              )
        """, nativeQuery = true)
    List<RegionLabelProjection> findLabelsByLevelInBounds(
            @Param("admLevel") String admLevel,
            @Param("south") double south,
            @Param("north") double north,
            @Param("west") double west,
            @Param("east") double east
    );

    interface RegionLabelProjection {
        String getName();
        double getLat();
        double getLng();
    }
}