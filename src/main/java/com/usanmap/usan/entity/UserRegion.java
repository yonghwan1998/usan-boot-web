package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_region")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "adm_cd", nullable = false, length = 10)
    private String admCd;

    @Column(name = "sido_name", nullable = false, length = 50)
    private String sidoName;

    @Column(name = "sigungu_name", nullable = false, length = 50)
    private String sigunguName;

    @Column(name = "emd_name", nullable = false, length = 50)
    private String emdName;

    @Column(name = "emd_lat", nullable = false, precision = 11, scale = 8)
    private BigDecimal emdLat;

    @Column(name = "emd_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal emdLng;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String admCd, String sidoName, String sigunguName, String emdName, BigDecimal emdLat, BigDecimal emdLng) {
        this.admCd = admCd;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.emdName = emdName;
        this.emdLat = emdLat;
        this.emdLng = emdLng;
    }
}
