package com.usanmap.usan.entity;

import com.usanmap.usan.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_product_id", nullable = false)
    private CreditProduct creditProduct;

    @Column(nullable = false, length = 50)
    private String productCodeSnapshot;

    @Column(nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(nullable = false)
    private Integer priceAmountSnapshot;

    @Column(nullable = false)
    private Integer baseCreditSnapshot;

    @Column(nullable = false)
    private Integer bonusCreditSnapshot;

    @Column(nullable = false)
    private Integer totalCreditSnapshot;

    @Column(length = 100)
    private String depositorName;

    @Column(length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.READY;
        }
        if (baseCreditSnapshot == null) {
            baseCreditSnapshot = 0;
        }
        if (bonusCreditSnapshot == null) {
            bonusCreditSnapshot = 0;
        }
        if (totalCreditSnapshot == null) {
            totalCreditSnapshot = (baseCreditSnapshot != null ? baseCreditSnapshot : 0)
                    + (bonusCreditSnapshot != null ? bonusCreditSnapshot : 0);
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
