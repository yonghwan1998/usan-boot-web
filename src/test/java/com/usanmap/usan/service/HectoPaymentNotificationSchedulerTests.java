package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentNotificationSchedulerTests {

    @Mock private HectoPaymentNotificationRepository notificationRepository;
    @Mock private HectoPaymentNotificationProcessor notificationProcessor;
    @Mock private PaymentObservability paymentObservability;

    private HectoPaymentNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new HectoPaymentNotificationScheduler(
                notificationRepository,
                notificationProcessor,
                paymentObservability
        );
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
    }

    @Test
    void recordsCommittedNotificationOutcomeAndStatusTransition() throws Exception {
        when(notificationRepository.findDueIds(
                eq(PaymentNotificationStatus.PENDING), any(LocalDateTime.class), any()
        )).thenReturn(List.of(1L));
        HectoChargeResult chargeResult = new HectoChargeResult(
                HectoChargeResult.Outcome.COMPLETED,
                OrderStatus.READY,
                OrderStatus.PAID
        );
        when(notificationProcessor.process(1L)).thenReturn(
                new HectoPaymentNotificationProcessor.ProcessingResult(
                        1L, "ORD-123", "TRD-123", 1, chargeResult
                )
        );

        scheduler.processPendingNotifications();

        verify(paymentObservability).recordNotificationProcessed(eq("completed"), any(Duration.class));
        verify(paymentObservability).recordPaymentStatusTransition("notification", "READY", "PAID");
    }

    @Test
    void recordsRetryAfterProcessingFailure() throws Exception {
        when(notificationRepository.findDueIds(
                eq(PaymentNotificationStatus.PENDING), any(LocalDateTime.class), any()
        )).thenReturn(List.of(1L));
        IllegalStateException failure = new IllegalStateException("lock timeout");
        when(notificationProcessor.process(1L)).thenThrow(failure);
        when(notificationProcessor.recordFailure(1L, failure)).thenReturn(
                new HectoPaymentNotificationProcessor.FailureResult(
                        1L,
                        "ORD-123",
                        "TRD-123",
                        2,
                        LocalDateTime.now().plusSeconds(30),
                        false
                )
        );

        scheduler.processPendingNotifications();

        verify(paymentObservability).recordNotificationProcessed(eq("error"), any(Duration.class));
        verify(paymentObservability).recordNotificationRetry("scheduled");
    }

    @Test
    void recordsExhaustedMetricWhenRetryLimitIsReached() throws Exception {
        when(notificationRepository.findDueIds(
                eq(PaymentNotificationStatus.PENDING), any(LocalDateTime.class), any()
        )).thenReturn(List.of(1L));
        IllegalStateException failure = new IllegalStateException("persistent failure");
        when(notificationProcessor.process(1L)).thenThrow(failure);
        when(notificationProcessor.recordFailure(1L, failure)).thenReturn(
                new HectoPaymentNotificationProcessor.FailureResult(
                        1L,
                        "ORD-123",
                        "TRD-123",
                        8,
                        LocalDateTime.now(),
                        true
                )
        );

        scheduler.processPendingNotifications();

        verify(paymentObservability).recordNotificationProcessed(eq("error"), any(Duration.class));
        verify(paymentObservability).recordNotificationRetry("exhausted");
    }
}
