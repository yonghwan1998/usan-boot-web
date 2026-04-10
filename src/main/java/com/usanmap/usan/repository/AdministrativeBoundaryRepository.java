package com.usanmap.usan.repository;

import com.usanmap.usan.entity.AdministrativeBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdministrativeBoundaryRepository extends JpaRepository<AdministrativeBoundary, Long> {

    @Query(value = """
        SELECT *
        FROM administrative_boundary ab
        WHERE ab.level = :level
          AND ST_Contains(
                ab.geom,
                ST_SRID(POINT(:lng, :lat), 4326)
          )
        LIMIT 1
        """, nativeQuery = true)
    Optional<AdministrativeBoundary> findContainingRegion(
            @Param("level") String level,
            @Param("lat") double lat,
            @Param("lng") double lng
    );
}