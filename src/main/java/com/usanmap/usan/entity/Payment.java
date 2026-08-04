package com.usanmap.usan.entity;

import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_order_id", nullable = false)
    private CreditOrder creditOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PgProvider pgProvider;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(length = 50)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private Integer requestedAmount;

    private Integer approvedAmount;

    @Column(length = 100)
    private String failureCode;

    @Column(length = 500)
    private String failureMessage;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    @Column(columnDefinition = "json")
    private String rawResponseJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.READY;
        }
        if (pgProvider == null) {
            pgProvider = PgProvider.HECTO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
