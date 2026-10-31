package com.usanmap.usan.entity;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_cancellation")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HectoPaymentProfiles.Environment environment;

    @Column(name = "merchant_cancellation_no", nullable = false, unique = true, length = 100)
    private String merchantCancellationNo;

    @Column(name = "original_transaction_no", nullable = false, length = 200)
    private String originalTransactionNo;

    @Column(name = "requested_amount", nullable = false)
    private Integer requestedAmount;

    @Column(name = "reserved_credit", nullable = false)
    private Integer reservedCredit;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentCancellationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "pg_cancellation_no", length = 200)
    private String pgCancellationNo;

    @Column(name = "result_code", length = 100)
    private String resultCode;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

    @Column(name = "raw_response_json", columnDefinition = "json")
    private String rawResponseJson;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = PaymentCancellationStatus.REQUESTED;
        if (attemptCount == null) attemptCount = 1;
        if (requestedAt == null) requestedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void retry(HectoPaymentProfiles.Environment environment, String merchantCancellationNo,
                      String reason, LocalDateTime requestedAt) {
        this.environment = environment;
        this.merchantCancellationNo = merchantCancellationNo;
        this.reason = reason;
        this.status = PaymentCancellationStatus.REQUESTED;
        this.attemptCount += 1;
        this.pgCancellationNo = null;
        this.resultCode = null;
        this.resultMessage = null;
        this.rawResponseJson = null;
        this.requestedAt = requestedAt;
        this.completedAt = null;
    }

    public void markProcessing() {
        requireStatus(PaymentCancellationStatus.REQUESTED);
        status = PaymentCancellationStatus.PROCESSING;
    }

    public void succeed(String pgCancellationNo, String resultCode, String resultMessage,
                        String rawResponseJson, LocalDateTime completedAt) {
        if (status != PaymentCancellationStatus.PROCESSING
                && status != PaymentCancellationStatus.REQUESTED
                && status != PaymentCancellationStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("성공 상태로 전환할 수 없는 취소 요청입니다.");
        }
        this.status = PaymentCancellationStatus.SUCCEEDED;
        this.pgCancellationNo = pgCancellationNo;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.rawResponseJson = rawResponseJson;
        this.completedAt = completedAt;
    }

    public void fail(String resultCode, String resultMessage, String rawResponseJson,
                     LocalDateTime completedAt) {
        requireStatus(PaymentCancellationStatus.PROCESSING);
        this.status = PaymentCancellationStatus.FAILED;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.rawResponseJson = rawResponseJson;
        this.completedAt = completedAt;
    }

    public void requireReview(String resultMessage, LocalDateTime completedAt) {
        if (status != PaymentCancellationStatus.PROCESSING
                && status != PaymentCancellationStatus.REQUESTED) {
            throw new IllegalStateException("검토 상태로 전환할 수 없는 취소 요청입니다.");
        }
        this.status = PaymentCancellationStatus.REVIEW_REQUIRED;
        this.resultMessage = resultMessage;
        this.completedAt = completedAt;
    }

    private void requireStatus(PaymentCancellationStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("결제 취소 상태가 올바르지 않습니다: " + status);
        }
    }
}
