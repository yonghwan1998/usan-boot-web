package world.usan.usan.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "listing_info")
public class CrawledListing {

    @Id
    @Column(name = "code", columnDefinition = "BINARY(16)")
    private UUID code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_code", foreignKey = @ForeignKey(name = "fk_listing_broker"))
    private CrawledBroker crawledBroker;

    @Column(name = "listing_type", length = 50, nullable = false)
    private String listingType;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal lng;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
