package world.usan.usan.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "broker_info", uniqueConstraints = @UniqueConstraint(name = "uk_regnum_name", columnNames = {"registration_number", "broker_name"}))
public class CrawledBroker {

    @Id
    @Column(name = "broker_code", columnDefinition = "BINARY(16)")
    private UUID brokerCode;

    @Column(name = "broker_name", length = 100, nullable = false)
    private String brokerName;

    @Column(name = "office_name", length = 150)
    private String officeName;

    @Column(name = "registration_number", length = 50, nullable = false)
    private String registrationNumber;

    @Column(name = "tel", length = 30)
    private String tel;

    @Column(name = "phone", length = 30, nullable = false)
    private String phone;

    @Column(name = "sido", length = 50)
    private String sido;

    @Column(name = "sigungu", length = 50)
    private String sigungu;

    @Column(name = "emd", length = 50)
    private String emd;

    @Column(name = "road_name", length = 100)
    private String roadName;

    @Column(name = "addr_road", length = 255)
    private String addrRoad;

    @Column(name = "addr_jibun", length = 255)
    private String addrJibun;

    @Column(name = "lat", precision = 10, scale = 7, nullable = false)
    private BigDecimal lat;

    @Column(name = "lng", precision = 10, scale = 7, nullable = false)
    private BigDecimal lng;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
