package com.usanmap.usan.service;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentStatusServiceTests {

    @Mock private CreditOrderRepository creditOrderRepository;
    @Mock private PaymentRepository paymentRepository;

    private PaymentStatusService paymentStatusService;

    @BeforeEach
    void setUp() {
        paymentStatusService = new PaymentStatusService(creditOrderRepository, paymentRepository);
    }

    @Test
    void expiresOnlyReadyHectoPaymentPastCutoff() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        CreditOrder order = CreditOrder.builder()
                .id(1L)
                .orderStatus(OrderStatus.READY)
                .orderedAt(cutoff.minusSeconds(1))
                .build();
        Payment payment = Payment.builder()
                .creditOrder(order)
                .pgProvider(PgProvider.HECTO)
                .paymentStatus(PaymentStatus.READY)
                .build();
        when(creditOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));

        paymentStatusService.expireHectoOrder(1L, cutoff);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void doesNotExpireBankTransferWaitingForApproval() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        CreditOrder order = CreditOrder.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING_BANK)
                .orderedAt(cutoff.minusDays(1))
                .build();
        when(creditOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        paymentStatusService.expireHectoOrder(1L, cutoff);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_BANK);
    }
}
