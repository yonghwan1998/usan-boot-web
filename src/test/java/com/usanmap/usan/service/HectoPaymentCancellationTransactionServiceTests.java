package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.CreditLedger;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.MemberCreditBalance;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.PaymentCancellation;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditLedgerRepository;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.MemberCreditBalanceRepository;
import com.usanmap.usan.repository.PaymentCancellationRepository;
import com.usanmap.usan.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentCancellationTransactionServiceTests {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentCancellationRepository cancellationRepository;
    @Mock private CreditOrderRepository orderRepository;
    @Mock private MemberCreditBalanceRepository balanceRepository;
    @Mock private CreditLedgerRepository ledgerRepository;

    private HectoPaymentCancellationTransactionService service;
    private User user;
    private CreditOrder order;
    private Payment payment;
    private MemberCreditBalance balance;
    private CreditLedger chargeLedger;
    private LocalDateTime requestedAt;

    @BeforeEach
    void setUp() {
        HectoPaymentProfiles profiles = new HectoPaymentProfiles(
                "test-mid", "test-hash", "12345678901234567890123456789012", true,
                "prod-mid", "prod-hash", "abcdefghijklmnopqrstuvwxyz123456", false
        );
        service = new HectoPaymentCancellationTransactionService(
                paymentRepository, cancellationRepository, orderRepository, balanceRepository,
                ledgerRepository, profiles
        );
        requestedAt = LocalDateTime.of(2026, 10, 31, 12, 0);
        user = User.builder().id(1L).email("test@naver.com").build();
        order = CreditOrder.builder()
                .id(2L).member(user).orderNo("ORDER1").orderStatus(OrderStatus.PAID)
                .priceAmountSnapshot(1000).totalCreditSnapshot(1100)
                .productNameSnapshot("크레딧 상품").paidAt(requestedAt.minusDays(1)).build();
        payment = Payment.builder()
                .id(3L).creditOrder(order).pgProvider(PgProvider.HECTO)
                .paymentStatus(PaymentStatus.SUCCESS).paymentKey("ORIGINAL1")
                .method("CA").approvedAmount(1000).build();
        balance = MemberCreditBalance.builder().id(4L).member(user).balance(2000).version(0L).build();
        chargeLedger = CreditLedger.builder()
                .id(5L).member(user).payment(payment).ledgerType(LedgerType.CHARGE)
                .createdAt(requestedAt.minusHours(12)).build();

        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByCreditOrder(order)).thenReturn(Optional.of(payment));
        when(cancellationRepository.findByPayment(payment)).thenReturn(Optional.empty());
        when(ledgerRepository.findFirstByPaymentAndLedgerTypeOrderByCreatedAtAsc(payment, LedgerType.CHARGE))
                .thenReturn(Optional.of(chargeLedger));
    }

    @Test
    void reservesAllGrantedCreditsBeforeCallingProvider() {
        when(ledgerRepository.existsByMemberAndLedgerTypeAndCreatedAtAfter(
                user, LedgerType.USE, chargeLedger.getCreatedAt())).thenReturn(false);
        when(balanceRepository.findByMemberWithLock(user)).thenReturn(Optional.of(balance));

        HectoPaymentCancellationTransactionService.PreparedCancellation result =
                service.prepare(3L, " 고객 요청 ", requestedAt);

        assertThat(balance.getBalance()).isEqualTo(900);
        assertThat(result.amount()).isEqualTo(1000);
        assertThat(result.reason()).isEqualTo("고객 요청");
        assertThat(result.profile().environment()).isEqualTo(HectoPaymentProfiles.Environment.TEST);
        ArgumentCaptor<PaymentCancellation> captor = ArgumentCaptor.forClass(PaymentCancellation.class);
        verify(cancellationRepository).save(captor.capture());
        assertThat(captor.getValue().getReservedCredit()).isEqualTo(1100);
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentCancellationStatus.REQUESTED);
    }

    @Test
    void rejectsAutomaticFullCancellationAfterAnyCreditUsage() {
        when(ledgerRepository.existsByMemberAndLedgerTypeAndCreatedAtAfter(
                user, LedgerType.USE, chargeLedger.getCreatedAt())).thenReturn(true);

        assertThatThrownBy(() -> service.prepare(3L, "고객 요청", requestedAt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("사용 이력");

        verify(balanceRepository, never()).findByMemberWithLock(any());
        verify(cancellationRepository, never()).save(any());
    }
}
