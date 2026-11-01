package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentStatusSchedulerTests {

    @Mock private CreditOrderRepository creditOrderRepository;
    @Mock private PaymentStatusService paymentStatusService;
    @Mock private PaymentObservability paymentObservability;

    private PaymentStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentStatusScheduler(
                creditOrderRepository,
                paymentStatusService,
                paymentObservability
        );
        ReflectionTestUtils.setField(scheduler, "readyExpirationMinutes", 30L);
        ReflectionTestUtils.setField(scheduler, "expirationBatchSize", 100);
    }

    @Test
    void recordsExpiredPaymentAfterCommittedTransition() {
        when(creditOrderRepository.findStaleHectoOrderIds(
                eq(OrderStatus.READY), any(LocalDateTime.class),
                eq(PgProvider.HECTO), eq(PaymentStatus.READY), any()
        )).thenReturn(List.of(1L));
        when(paymentStatusService.expireHectoOrder(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new PaymentStatusService.ExpirationResult(1L, "ORD-123", 2L));

        scheduler.expireStaleHectoOrders();

        verify(paymentObservability).recordPaymentExpired();
        verify(paymentObservability).recordPaymentStatusTransition("timeout", "READY", "EXPIRED");
    }
}
