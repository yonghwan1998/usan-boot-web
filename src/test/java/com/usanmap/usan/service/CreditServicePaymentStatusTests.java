package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.CreditLedger;
import com.usanmap.usan.entity.MemberCreditBalance;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditLedgerRepository;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.CreditProductRepository;
import com.usanmap.usan.repository.MemberCreditBalanceRepository;
import com.usanmap.usan.repository.PaymentRepository;
import com.usanmap.usan.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServicePaymentStatusTests {

    @Mock private CreditProductRepository creditProductRepository;
    @Mock private CreditOrderRepository creditOrderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MemberCreditBalanceRepository memberCreditBalanceRepository;
    @Mock private CreditLedgerRepository creditLedgerRepository;
    @Mock private UserRepository userRepository;

    private CreditService creditService;

    @BeforeEach
    void setUp() {
        creditService = new CreditService(
                creditProductRepository,
                creditOrderRepository,
                paymentRepository,
                memberCreditBalanceRepository,
                creditLedgerRepository,
                userRepository,
                new ObjectMapper()
        );
    }

    @Test
    void recordsSignedPgFailureAsProcessedPaymentFailure() {
        CreditOrder order = order(OrderStatus.READY);
        Payment payment = payment(order, PaymentStatus.READY);
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        Map<String, String> result = new HashMap<>();
        result.put("mchtTrdNo", "ORD-123");
        result.put("outStatCd", "0031");
        result.put("outRsltCd", "CA01");
        result.put("outRsltMsg", "승인 거절");

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("CA01");
        assertThat(payment.getFailureMessage()).isEqualTo("승인 거절");
        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.FAILED);
        assertThat(chargeResult.previousStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void recordsMalformedAmountAsPaymentFailure() {
        CreditOrder order = order(OrderStatus.READY);
        Payment payment = payment(order, PaymentStatus.READY);
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0021",
                "trdAmt", "invalid"
        );

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureMessage()).isEqualTo("결제 금액 형식이 올바르지 않습니다.");
        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.FAILED);
    }

    @Test
    void lateSuccessNotificationRecoversExpiredPayment() {
        CreditOrder order = order(OrderStatus.EXPIRED);
        Payment payment = payment(order, PaymentStatus.EXPIRED);
        MemberCreditBalance balance = MemberCreditBalance.builder()
                .member(order.getMember())
                .balance(50)
                .build();
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        when(memberCreditBalanceRepository.findByMemberWithLock(order.getMember()))
                .thenReturn(Optional.of(balance));
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0021",
                "trdAmt", "1000",
                "trdNo", "TRD-123",
                "method", "CA"
        );

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(balance.getBalance()).isEqualTo(150);
        verify(creditLedgerRepository).save(any());
        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.RECOVERED);
        assertThat(chargeResult.previousStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void successfulNotificationCreditsBalanceAndCreatesLedger() {
        CreditOrder order = order(OrderStatus.READY);
        Payment payment = payment(order, PaymentStatus.READY);
        MemberCreditBalance balance = MemberCreditBalance.builder()
                .member(order.getMember())
                .balance(25)
                .build();
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        when(memberCreditBalanceRepository.findByMemberWithLock(order.getMember()))
                .thenReturn(Optional.of(balance));
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0021",
                "trdAmt", "1000",
                "trdNo", "TRD-123",
                "method", "CA"
        );

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentKey()).isEqualTo("TRD-123");
        assertThat(payment.getMethod()).isEqualTo("CA");
        assertThat(payment.getApprovedAmount()).isEqualTo(1000);
        assertThat(balance.getBalance()).isEqualTo(125);
        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.COMPLETED);

        ArgumentCaptor<CreditLedger> ledgerCaptor = ArgumentCaptor.forClass(CreditLedger.class);
        verify(creditLedgerRepository).save(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getChangeAmount()).isEqualTo(100);
        assertThat(ledgerCaptor.getValue().getBalanceAfter()).isEqualTo(125);
    }

    @Test
    void duplicateSuccessNotificationDoesNotCreditAgain() {
        CreditOrder order = order(OrderStatus.PAID);
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(Map.of("mchtTrdNo", "ORD-123"));

        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.ALREADY_PROCESSED);
        assertThat(chargeResult.previousStatus()).isEqualTo(OrderStatus.PAID);
        verifyNoInteractions(paymentRepository, memberCreditBalanceRepository, creditLedgerRepository);
    }

    @Test
    void mismatchedAmountFailsWithoutCreditingBalance() {
        CreditOrder order = order(OrderStatus.READY);
        Payment payment = payment(order, PaymentStatus.READY);
        when(creditOrderRepository.findByOrderNoForUpdate("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0021",
                "trdAmt", "999",
                "trdNo", "TRD-123"
        );

        HectoChargeResult chargeResult = creditService.confirmHectoCharge(result);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureMessage()).isEqualTo("결제 금액이 일치하지 않습니다.");
        assertThat(chargeResult.outcome()).isEqualTo(HectoChargeResult.Outcome.FAILED);
        verify(memberCreditBalanceRepository, never()).findByMemberWithLock(any());
        verify(creditLedgerRepository, never()).save(any());
    }

    private CreditOrder order(OrderStatus status) {
        return CreditOrder.builder()
                .orderNo("ORD-123")
                .member(User.builder().id(1L).build())
                .priceAmountSnapshot(1000)
                .totalCreditSnapshot(100)
                .productNameSnapshot("100 크레딧")
                .orderStatus(status)
                .build();
    }

    private Payment payment(CreditOrder order, PaymentStatus status) {
        return Payment.builder()
                .creditOrder(order)
                .pgProvider(PgProvider.HECTO)
                .paymentStatus(status)
                .requestedAmount(1000)
                .build();
    }
}
