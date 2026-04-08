package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private Integer priceAmount;

    @Column(nullable = false)
    private Integer baseCreditAmount;

    @Column(nullable = false)
    private Integer bonusCreditAmount;

    @Column(nullable = false)
    private Integer totalCreditAmount;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (baseCreditAmount == null) {
            baseCreditAmount = 0;
        }
        if (bonusCreditAmount == null) {
            bonusCreditAmount = 0;
        }
        if (totalCreditAmount == null) {
            totalCreditAmount = (baseCreditAmount != null ? baseCreditAmount : 0)
                    + (bonusCreditAmount != null ? bonusCreditAmount : 0);
        }
        if (isActive == null) {
            isActive = true;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        totalCreditAmount = baseCreditAmount + bonusCreditAmount;
    }
}
