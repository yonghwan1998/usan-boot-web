package com.usanmap.usan.repository;

import com.usanmap.usan.entity.AdministrativeBoundary;
import com.usanmap.usan.entity.enums.AdministrativeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdministrativeBoundaryRepository extends JpaRepository<AdministrativeBoundary, Long> {

    Optional<AdministrativeBoundary> findByAdmCd(String admCd);

    Optional<AdministrativeBoundary> findFirstByAdmLevelAndAdmCdStartingWith(AdministrativeLevel admLevel, String prefix);

    @Query(value = """
        SELECT ab.adm_cd AS admCd, ab.name AS name
        FROM administrative_boundary ab
        WHERE ab.adm_level = :admLevel
        ORDER BY ab.name
        """, nativeQuery = true)
    List<RegionSelectProjection> findNamesByLevel(@Param("admLevel") String admLevel);

    @Query(value = """
        SELECT ab.adm_cd AS admCd, ab.name AS name
        FROM administrative_boundary ab
        WHERE ab.adm_level = :admLevel
          AND ab.parent_adm_cd = :parentAdmCd
        ORDER BY ab.name
        """, nativeQuery = true)
    List<RegionSelectProjection> findNamesByLevelAndParent(
            @Param("admLevel") String admLevel,
            @Param("parentAdmCd") String parentAdmCd);

    interface RegionSelectProjection {
        String getAdmCd();
        String getName();
    }

    @Query(value = """
        SELECT ab.adm_cd                                                         AS admCd,
               ab.name                                                           AS name,
               ST_Y(ST_Centroid(ST_GeomFromWKB(ST_AsBinary(ab.geom))))          AS lat,
               ST_X(ST_Centroid(ST_GeomFromWKB(ST_AsBinary(ab.geom))))          AS lng
        FROM administrative_boundary ab
        WHERE ab.adm_level = 'EMD'
          AND ab.parent_adm_cd = :sigunguCd
        ORDER BY ab.name
        """, nativeQuery = true)
    List<EmdItemProjection> findEmdListByParentAdmCd(@Param("sigunguCd") String sigunguCd);

    interface EmdItemProjection {
        String getAdmCd();
        String getName();
        double getLat();
        double getLng();
    }

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