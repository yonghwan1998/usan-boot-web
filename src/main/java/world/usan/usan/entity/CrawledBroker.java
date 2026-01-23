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
    @Column(columnDefinition = "BINARY(16)")
    private UUID brokerCode;

    @Column(length = 100, nullable = false)
    private String brokerName;

    @Column(length = 150)
    private String officeName;

    @Column(length = 50, nullable = false)
    private String registrationNumber;

    @Column(length = 30)
    private String tel;

    @Column(length = 30, nullable = false)
    private String phone;

    @Column(length = 255)
    private String roadAddress;

    @Column(length = 255)
    private String jibunAddress;

    @Column(length = 50)
    private String sido;

    @Column(length = 50)
    private String sigungu;

    @Column(length = 50)
    private String emd;

    @Column(length = 50)
    private String ri;

    @Column(length = 50)
    private String roadName;

    @Column(length = 50)
    private String buildingNumber;

    @Column(length = 100)
    private String buildingName;

    @Column(length = 50)
    private String landNumber;

    @Column(length = 50)
    private String postalCode;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal lng;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
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
