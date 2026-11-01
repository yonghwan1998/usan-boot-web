package com.usanmap.usan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.HectoPaymentNotification;
import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HectoPaymentNotificationProcessor {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};

    private final HectoPaymentNotificationRepository notificationRepository;
    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    @Value("${hecto.notification.max-attempts:8}")
    private int maxAttempts;

    @Value("${hecto.notification.retry-base-seconds:30}")
    private long retryBaseSeconds;

    @Value("${hecto.notification.retry-max-seconds:3600}")
    private long retryMaxSeconds;

    /** 노티 상태와 결제/크레딧 반영을 하나의 트랜잭션으로 처리한다. */
    @Transactional
    public ProcessingResult process(Long notificationId) throws Exception {
        HectoPaymentNotification notification = notificationRepository.findByIdForUpdate(notificationId).orElse(null);
        if (notification == null || notification.getStatus() != PaymentNotificationStatus.PENDING
                || notification.getNextAttemptAt().isAfter(LocalDateTime.now())) {
            return null;
        }

        Map<String, String> result = objectMapper.readValue(notification.getPayloadJson(), STRING_MAP_TYPE);
        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);
        notification.markProcessed(LocalDateTime.now());
        return new ProcessingResult(
                notification.getId(),
                notification.getOrderNo(),
                notification.getTransactionNo(),
                notification.getAttemptCount(),
                chargeResult
        );
    }

    /** 처리 트랜잭션이 롤백된 뒤 별도 트랜잭션에서 재시도 일정을 기록한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailureResult recordFailure(Long notificationId, Exception cause) {
        HectoPaymentNotification notification = notificationRepository.findByIdForUpdate(notificationId).orElse(null);
        if (notification == null || notification.getStatus() != PaymentNotificationStatus.PENDING) {
            return null;
        }

        int nextAttemptCount = notification.getAttemptCount() + 1;
        String error = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        LocalDateTime now = LocalDateTime.now();
        if (nextAttemptCount >= maxAttempts) {
            notification.markTerminalFailure(error, now);
            return new FailureResult(
                    notification.getId(),
                    notification.getOrderNo(),
                    notification.getTransactionNo(),
                    notification.getAttemptCount(),
                    notification.getNextAttemptAt(),
                    true
            );
        }

        long multiplier = 1L << Math.min(nextAttemptCount - 1, 20);
        long delaySeconds = Math.min(retryBaseSeconds * multiplier, retryMaxSeconds);
        notification.scheduleRetry(error, now.plusSeconds(delaySeconds));
        return new FailureResult(
                notification.getId(),
                notification.getOrderNo(),
                notification.getTransactionNo(),
                notification.getAttemptCount(),
                notification.getNextAttemptAt(),
                false
        );
    }

    public record ProcessingResult(
            Long notificationId,
            String orderNo,
            String transactionNo,
            int attemptCount,
            HectoChargeResult chargeResult
    ) {}

    public record FailureResult(
            Long notificationId,
            String orderNo,
            String transactionNo,
            int attemptCount,
            LocalDateTime nextAttemptAt,
            boolean exhausted
    ) {}
}
