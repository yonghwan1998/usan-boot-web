package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.PaymentCancellation;
import com.usanmap.usan.entity.PaymentReconciliationRun;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentReconciliationStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.PaymentReconciliationRunRepository;
import com.usanmap.usan.repository.PaymentCancellationRepository;
import com.usanmap.usan.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentReconciliationServiceTests {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 30);

    @Mock private HectoReconciliationClient reconciliationClient;
    @Mock private PaymentReconciliationRunRepository runRepository;
    @Mock private CreditOrderRepository creditOrderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentCancellationRepository cancellationRepository;
    @Mock private CreditService creditService;
    @Mock private HectoPaymentCancellationTransactionService cancellationTransactionService;
    @Mock private PaymentObservability paymentObservability;

    private HectoPaymentProfiles profiles;
    private HectoPaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        profiles = new HectoPaymentProfiles(
                "test-merchant", "test-hash", "12345678901234567890123456789012", true,
                "prod-merchant", "prod-hash", "abcdefghijklmnopqrstuvwxyz123456", false
        );
        service = new HectoPaymentReconciliationService(
                reconciliationClient,
                profiles,
                runRepository,
                creditOrderRepository,
                paymentRepository,
                cancellationRepository,
                creditService,
                cancellationTransactionService,
                paymentObservability
        );
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void dryRunReportsRecoveryCandidateWithoutChangingPayment() {
        CreditOrder order = order(OrderStatus.EXPIRED, "test@naver.com");
        Payment payment = payment(order, PaymentStatus.EXPIRED);
        stubApproval(order, payment);

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, false
        );

        assertThat(summary.recoveredCount()).isZero();
        assertThat(summary.mismatchCount()).isEqualTo(1);
        verify(creditService, never()).confirmHectoCharge(any());
    }

    @Test
    void recoversApprovedExpiredPaymentWhenRecoveryIsEnabled() {
        CreditOrder order = order(OrderStatus.EXPIRED, "test@naver.com");
        Payment payment = payment(order, PaymentStatus.EXPIRED);
        stubApproval(order, payment);
        when(creditService.confirmHectoCharge(any())).thenReturn(new HectoChargeResult(
                HectoChargeResult.Outcome.RECOVERED,
                OrderStatus.EXPIRED,
                OrderStatus.PAID
        ));

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, true
        );

        assertThat(summary.recoveredCount()).isEqualTo(1);
        assertThat(summary.mismatchCount()).isZero();
        verify(paymentObservability).recordPaymentReconciliationRecovered("test");
        verify(creditService).confirmHectoCharge(any());
    }

    @Test
    void countsMatchingPaidPaymentWithoutChangingIt() {
        CreditOrder order = order(OrderStatus.PAID, "test@naver.com");
        Payment payment = payment(order, PaymentStatus.SUCCESS);
        payment.setPaymentKey("TRD-123");
        payment.setApprovedAmount(1000);
        stubApproval(order, payment);

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, true
        );

        assertThat(summary.matchedCount()).isEqualTo(1);
        assertThat(summary.recoveredCount()).isZero();
        verify(creditService, never()).confirmHectoCharge(any());
    }

    @Test
    void doesNotRecoverApprovalThatWasCanceledInSameReport() {
        CreditOrder order = order(OrderStatus.EXPIRED, "test@naver.com");
        HectoReconciliationTransaction cancellation = new HectoReconciliationTransaction(
                "test-merchant", "CA", "TRD-CANCEL", "ORD-123",
                HectoReconciliationTransaction.Type.CANCELLATION,
                TARGET_DATE, LocalTime.NOON, 1000, "TRD-123"
        );
        when(reconciliationClient.fetch(any(), eq(TARGET_DATE))).thenReturn(
                new HectoReconciliationResponse("0000", "정상", List.of(approval(), cancellation))
        );
        when(creditOrderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));
        when(paymentRepository.findApprovedForReconciliation(any(), any(), any(), any()))
                .thenReturn(List.of());

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, true
        );

        assertThat(summary.cancellationCount()).isEqualTo(1);
        assertThat(summary.mismatchCount()).isEqualTo(1);
        assertThat(summary.recoveredCount()).isZero();
        verify(creditService, never()).confirmHectoCharge(any());
    }

    @Test
    void completesUncertainCancellationOnlyWhenReconciliationRecoveryIsEnabled() {
        CreditOrder order = order(OrderStatus.PAID, "test@naver.com");
        Payment payment = payment(order, PaymentStatus.SUCCESS);
        PaymentCancellation cancellation = PaymentCancellation.builder()
                .id(9L)
                .payment(payment)
                .environment(HectoPaymentProfiles.Environment.TEST)
                .merchantCancellationNo("CANCEL-1")
                .originalTransactionNo("TRD-123")
                .requestedAmount(1000)
                .reservedCredit(100)
                .reason("고객 요청")
                .status(PaymentCancellationStatus.REVIEW_REQUIRED)
                .attemptCount(1)
                .build();
        HectoReconciliationTransaction transaction = new HectoReconciliationTransaction(
                "test-merchant", "CA", "TRD-CANCEL", "CANCEL-1",
                HectoReconciliationTransaction.Type.CANCELLATION,
                TARGET_DATE, LocalTime.NOON, 1000, "TRD-123"
        );
        when(reconciliationClient.fetch(any(), eq(TARGET_DATE))).thenReturn(
                new HectoReconciliationResponse("0000", "정상", List.of(transaction))
        );
        when(cancellationRepository.findByMerchantCancellationNo("CANCEL-1"))
                .thenReturn(Optional.of(cancellation));
        when(cancellationTransactionService.completeFromReconciliation(9L, "TRD-CANCEL", "TRD-123", 1000))
                .thenReturn(HectoPaymentCancellationTransactionService.ReconciliationOutcome.COMPLETED);
        when(paymentRepository.findApprovedForReconciliation(any(), any(), any(), any()))
                .thenReturn(List.of());

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, true
        );

        assertThat(summary.recoveredCount()).isEqualTo(1);
        assertThat(summary.mismatchCount()).isZero();
        verify(cancellationTransactionService)
                .completeFromReconciliation(9L, "TRD-CANCEL", "TRD-123", 1000);
    }

    @Test
    void recordsFailedRunWhenProviderQueryFails() {
        when(reconciliationClient.fetch(any(), eq(TARGET_DATE)))
                .thenThrow(new IllegalStateException("provider unavailable"));

        assertThatThrownBy(() -> service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, false
        )).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<PaymentReconciliationRun> runCaptor = ArgumentCaptor.forClass(PaymentReconciliationRun.class);
        verify(runRepository, org.mockito.Mockito.atLeast(2)).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(PaymentReconciliationStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage()).contains("provider unavailable");
        verify(paymentObservability).recordPaymentReconciliationRun("test", "failed");
    }

    @Test
    void doesNotRecoverUnsupportedPaymentMethod() {
        HectoReconciliationTransaction transfer = new HectoReconciliationTransaction(
                "test-merchant", "RA", "TRD-TRANSFER", "ORD-123",
                HectoReconciliationTransaction.Type.APPROVAL,
                TARGET_DATE, LocalTime.NOON, 1000, ""
        );
        when(reconciliationClient.fetch(any(), eq(TARGET_DATE))).thenReturn(
                new HectoReconciliationResponse("0000", "정상", List.of(transfer))
        );
        when(paymentRepository.findApprovedForReconciliation(any(), any(), any(), any()))
                .thenReturn(List.of());

        HectoPaymentReconciliationService.ReconciliationSummary summary = service.reconcile(
                TARGET_DATE, HectoPaymentProfiles.Environment.TEST, true
        );

        assertThat(summary.mismatchCount()).isEqualTo(1);
        verify(creditService, never()).confirmHectoCharge(any());
    }

    private void stubApproval(CreditOrder order, Payment payment) {
        HectoReconciliationTransaction approval = approval();
        when(reconciliationClient.fetch(any(), eq(TARGET_DATE))).thenReturn(
                new HectoReconciliationResponse("0000", "정상", List.of(approval))
        );
        when(creditOrderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));
        when(creditOrderRepository.findMemberEmailByOrderNo("ORD-123"))
                .thenReturn(Optional.of(order.getMember().getEmail()));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        when(paymentRepository.findApprovedForReconciliation(any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    private HectoReconciliationTransaction approval() {
        return new HectoReconciliationTransaction(
                "test-merchant", "CA", "TRD-123", "ORD-123",
                HectoReconciliationTransaction.Type.APPROVAL,
                TARGET_DATE, LocalTime.NOON, 1000, ""
        );
    }

    private CreditOrder order(OrderStatus status, String email) {
        return CreditOrder.builder()
                .id(1L)
                .orderNo("ORD-123")
                .member(User.builder().id(1L).email(email).build())
                .priceAmountSnapshot(1000)
                .totalCreditSnapshot(100)
                .productNameSnapshot("100 크레딧")
                .orderStatus(status)
                .build();
    }

    private Payment payment(CreditOrder order, PaymentStatus status) {
        return Payment.builder()
                .id(1L)
                .creditOrder(order)
                .pgProvider(PgProvider.HECTO)
                .paymentStatus(status)
                .requestedAmount(1000)
                .approvedAt(LocalDateTime.of(TARGET_DATE, LocalTime.NOON))
                .build();
    }
}
