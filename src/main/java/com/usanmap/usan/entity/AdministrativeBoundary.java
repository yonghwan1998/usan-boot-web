package com.usanmap.usan.entity;

import com.usanmap.usan.entity.enums.AdministrativeLevel;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

@Entity
@Table(name = "administrative_boundary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adm_cd", nullable = false, unique = true, length = 10)
    private String admCd;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private AdministrativeLevel level;

    @Column(name = "parent_adm_cd", length = 10)
    private String parentAdmCd;

    @Column(name = "geom", nullable = false, columnDefinition = "geometry SRID 4326")
    private Geometry geom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}