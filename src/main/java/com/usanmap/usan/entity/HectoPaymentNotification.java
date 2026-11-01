package com.usanmap.usan.entity;

import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "hecto_payment_notification",
        uniqueConstraints = @UniqueConstraint(name = "uk_hecto_payment_notification_event_key", columnNames = "event_key"),
        indexes = @Index(name = "idx_hecto_payment_notification_due", columnList = "status,next_attempt_at,id")
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HectoPaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "order_no", nullable = false, length = 100)
    private String orderNo;

    @Column(name = "transaction_no", nullable = false, length = 40)
    private String transactionNo;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentNotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = PaymentNotificationStatus.PENDING;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markProcessed(LocalDateTime processedAt) {
        attemptCount++;
        status = PaymentNotificationStatus.PROCESSED;
        this.processedAt = processedAt;
        nextAttemptAt = processedAt;
        lastError = null;
    }

    public void markTerminalFailure(String error, LocalDateTime failedAt) {
        attemptCount++;
        status = PaymentNotificationStatus.FAILED;
        nextAttemptAt = failedAt;
        lastError = truncate(error);
    }

    public void scheduleRetry(String error, LocalDateTime nextAttemptAt) {
        attemptCount++;
        status = PaymentNotificationStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        lastError = truncate(error);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
