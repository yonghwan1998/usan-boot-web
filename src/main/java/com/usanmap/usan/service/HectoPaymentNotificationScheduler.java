package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class HectoPaymentNotificationScheduler {

    private final HectoPaymentNotificationRepository notificationRepository;
    private final HectoPaymentNotificationProcessor notificationProcessor;
    private final PaymentObservability paymentObservability;

    @Value("${hecto.notification.batch-size:20}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${hecto.notification.processing-delay-ms:1000}",
            initialDelayString = "${hecto.notification.initial-delay-ms:1000}"
    )
    public void processPendingNotifications() {
        List<Long> notificationIds = notificationRepository.findDueIds(
                PaymentNotificationStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (Long notificationId : notificationIds) {
            long startedAt = System.nanoTime();
            try {
                HectoPaymentNotificationProcessor.ProcessingResult result =
                        notificationProcessor.process(notificationId);
                if (result == null) {
                    continue;
                }
                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                String outcome = result.chargeResult().outcome().name().toLowerCase(Locale.ROOT);
                paymentObservability.recordNotificationProcessed(outcome, duration);
                log.info("payment_notification_processed notificationId={} orderNo={} transactionNo={} " +
                                "attempt={} outcome={} durationMs={}",
                        result.notificationId(), result.orderNo(), result.transactionNo(),
                        result.attemptCount(), outcome, duration.toMillis());
                HectoChargeResult chargeResult = result.chargeResult();
                if (chargeResult.previousStatus() != chargeResult.currentStatus()) {
                    paymentObservability.recordPaymentStatusTransition(
                            "notification",
                            chargeResult.previousStatus().name(),
                            chargeResult.currentStatus().name()
                    );
                }
                switch (chargeResult.outcome()) {
                    case COMPLETED -> log.info(
                            "payment_status_changed orderNo={} transactionNo={} from=READY to=PAID source=notification",
                            result.orderNo(), result.transactionNo());
                    case RECOVERED -> log.info(
                            "payment_status_changed orderNo={} transactionNo={} from=EXPIRED to=PAID source=notification",
                            result.orderNo(), result.transactionNo());
                    case FAILED -> log.info(
                            "payment_status_changed orderNo={} transactionNo={} from={} to=FAILED source=notification",
                            result.orderNo(), result.transactionNo(), chargeResult.previousStatus());
                    case ALREADY_PROCESSED -> log.info(
                            "payment_notification_idempotent orderNo={} transactionNo={}",
                            result.orderNo(), result.transactionNo());
                }
            } catch (Exception e) {
                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                paymentObservability.recordNotificationProcessed("error", duration);
                try {
                    HectoPaymentNotificationProcessor.FailureResult failure =
                            notificationProcessor.recordFailure(notificationId, e);
                    if (failure == null) {
                        continue;
                    }
                    String retryOutcome = failure.exhausted() ? "exhausted" : "scheduled";
                    paymentObservability.recordNotificationRetry(retryOutcome);
                    if (failure.exhausted()) {
                        log.error("payment_notification_exhausted notificationId={} orderNo={} transactionNo={} " +
                                        "attempt={} exceptionType={} durationMs={}",
                                failure.notificationId(), failure.orderNo(), failure.transactionNo(),
                                failure.attemptCount(), e.getClass().getSimpleName(), duration.toMillis(), e);
                    } else {
                        log.warn("payment_notification_retry notificationId={} orderNo={} transactionNo={} " +
                                        "attempt={} nextAttemptAt={} exceptionType={} durationMs={}",
                                failure.notificationId(), failure.orderNo(), failure.transactionNo(),
                                failure.attemptCount(), failure.nextAttemptAt(),
                                e.getClass().getSimpleName(), duration.toMillis());
                    }
                } catch (Exception recordFailureException) {
                    log.error("payment_notification_retry_record_failed notificationId={} exceptionType={}",
                            notificationId, recordFailureException.getClass().getSimpleName(), recordFailureException);
                }
            }
        }
    }
}
