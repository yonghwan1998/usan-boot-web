package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;
import com.usanmap.usan.dto.ListingRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.usanmap.usan.util.NumberUtils.toBigDecimal;
import static com.usanmap.usan.util.PublicIdUtils.generate;

@Entity
@Table(name = "listing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", length = 6, nullable = false, unique = true)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "address_name", nullable = false, length = 255)
    private String addressName;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "owner_name", length = 50)
    private String ownerName;

    @Column(length = 30)
    private String carrier;

    @Column(name = "owner_phone", length = 20)
    private String ownerPhone;

    @Column(name = "trade_type", length = 30)
    private String tradeType;

    @Column(name = "deposit_manwon")
    private Integer depositManwon;

    @Column(name = "rent_manwon")
    private Integer rentManwon;

    @Column(name = "price_manwon")
    private Integer priceManwon;

    @Column(length = 50)
    private String dongho;

    @Column(name = "floor_info", length = 50)
    private String floorInfo;

    @Column(name = "area_m2", precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Lob
    private String description;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Listing(String publicId, Long userId, String role, String type,
                    String addressName, String roadAddress, String jibunAddress, String addressDetail,
                    BigDecimal lat, BigDecimal lng,
                    String ownerName, String carrier, String ownerPhone,
                    String tradeType, Integer depositManwon, Integer rentManwon, Integer priceManwon,
                    String dongho, String floorInfo, BigDecimal areaM2, String description,
                    String status) {

        this.publicId = publicId;
        this.userId = userId;
        this.role = role;
        this.type = type;

        this.addressName = addressName;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.addressDetail = addressDetail;
        this.lat = lat;
        this.lng = lng;

        this.ownerName = ownerName;
        this.carrier = carrier;
        this.ownerPhone = ownerPhone;

        this.tradeType = tradeType;
        this.depositManwon = depositManwon;
        this.rentManwon = rentManwon;
        this.priceManwon = priceManwon;

        this.dongho = dongho;
        this.floorInfo = floorInfo;
        this.areaM2 = areaM2;
        this.description = description;

        this.status = (status == null ? "DRAFT" : status);
    }

    public static Listing createDraft(String publicId, ListingRequest req, Long userId) {
        return Listing.builder()
                .publicId(publicId)
                .userId(userId)
                .role(req.role() == null ? null : req.role().name())
                .type(req.type() == null ? null : req.type().name())
                .addressName(req.addressName())
                .roadAddress(req.roadAddress())
                .jibunAddress(req.jibunAddress())
                .addressDetail(req.addressDetail())
                .lat(toBigDecimal(req.lat()))
                .lng(toBigDecimal(req.lng()))
                .ownerName(req.ownerName())
                .carrier(req.carrier())
                .ownerPhone(req.ownerPhone())
                .tradeType(req.tradeType() == null ? null : req.tradeType().name())
                .depositManwon(req.depositManwon())
                .rentManwon(req.rentManwon())
                .priceManwon(req.priceManwon())
                .dongho(req.dongho())
                .floorInfo(req.floorInfo())
                .areaM2(toBigDecimal(req.areaM2()))
                .description(req.description())
                .status("DRAFT")
                .build();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "DRAFT";
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}