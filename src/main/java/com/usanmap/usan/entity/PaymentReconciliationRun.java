package com.usanmap.usan.entity;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.enums.PaymentReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_reconciliation_run")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HectoPaymentProfiles.Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentReconciliationStatus status;

    @Column(name = "provider_count", nullable = false)
    private Integer providerCount;

    @Column(name = "matched_count", nullable = false)
    private Integer matchedCount;

    @Column(name = "recovered_count", nullable = false)
    private Integer recoveredCount;

    @Column(name = "mismatch_count", nullable = false)
    private Integer mismatchCount;

    @Column(name = "cancellation_count", nullable = false)
    private Integer cancellationCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = PaymentReconciliationStatus.RUNNING;
        }
        if (providerCount == null) providerCount = 0;
        if (matchedCount == null) matchedCount = 0;
        if (recoveredCount == null) recoveredCount = 0;
        if (mismatchCount == null) mismatchCount = 0;
        if (cancellationCount == null) cancellationCount = 0;
        if (startedAt == null) startedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void complete(int providerCount, int matchedCount, int recoveredCount,
                         int mismatchCount, int cancellationCount) {
        this.status = PaymentReconciliationStatus.COMPLETED;
        this.providerCount = providerCount;
        this.matchedCount = matchedCount;
        this.recoveredCount = recoveredCount;
        this.mismatchCount = mismatchCount;
        this.cancellationCount = cancellationCount;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(Exception exception) {
        this.status = PaymentReconciliationStatus.FAILED;
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        this.errorMessage = message.length() <= 1000 ? message : message.substring(0, 1000);
        this.completedAt = LocalDateTime.now();
    }
}
