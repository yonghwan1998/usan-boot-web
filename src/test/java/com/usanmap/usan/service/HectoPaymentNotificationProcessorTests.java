package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.HectoPaymentNotification;
import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentNotificationProcessorTests {

    @Mock private HectoPaymentNotificationRepository notificationRepository;
    @Mock private CreditService creditService;

    private HectoPaymentNotificationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new HectoPaymentNotificationProcessor(notificationRepository, creditService, new ObjectMapper());
        ReflectionTestUtils.setField(processor, "maxAttempts", 3);
        ReflectionTestUtils.setField(processor, "retryBaseSeconds", 30L);
        ReflectionTestUtils.setField(processor, "retryMaxSeconds", 3600L);
    }

    @Test
    void marksNotificationProcessedInSameUnitAsCreditConfirmation() throws Exception {
        HectoPaymentNotification notification = pendingNotification();
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123", "trdNo", "TRD-123");
        when(notificationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(notification));
        when(creditService.confirmHectoCharge(result)).thenReturn(true);

        processor.process(1L);

        assertThat(notification.getStatus()).isEqualTo(PaymentNotificationStatus.PROCESSED);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getProcessedAt()).isNotNull();
    }

    @Test
    void processingExceptionIsLeftPendingForFailureRecorder() {
        HectoPaymentNotification notification = pendingNotification();
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123", "trdNo", "TRD-123");
        when(notificationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(notification));
        when(creditService.confirmHectoCharge(result)).thenThrow(new IllegalStateException("lock timeout"));

        assertThatThrownBy(() -> processor.process(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("lock timeout");

        assertThat(notification.getStatus()).isEqualTo(PaymentNotificationStatus.PENDING);
        assertThat(notification.getAttemptCount()).isZero();
    }

    @Test
    void schedulesRetryWithBackoffAfterProcessingRollback() {
        HectoPaymentNotification notification = pendingNotification();
        LocalDateTime before = LocalDateTime.now();
        when(notificationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(notification));

        processor.recordFailure(1L, new IllegalStateException("lock timeout"));

        assertThat(notification.getStatus()).isEqualTo(PaymentNotificationStatus.PENDING);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getNextAttemptAt()).isAfterOrEqualTo(before.plusSeconds(30));
        assertThat(notification.getLastError()).contains("lock timeout");
    }

    private HectoPaymentNotification pendingNotification() {
        return HectoPaymentNotification.builder()
                .id(1L)
                .eventKey("TRD-123")
                .orderNo("ORD-123")
                .transactionNo("TRD-123")
                .payloadJson("{\"mchtTrdNo\":\"ORD-123\",\"trdNo\":\"TRD-123\"}")
                .status(PaymentNotificationStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now().minusSeconds(1))
                .receivedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
