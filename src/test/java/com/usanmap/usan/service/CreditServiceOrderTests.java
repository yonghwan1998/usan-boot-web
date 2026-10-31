package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.CreditProduct;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceOrderTests {

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
    void createsReadyHectoOrderAndPaymentFromProductSnapshot() {
        User user = User.builder().id(1L).email("member@example.com").build();
        CreditProduct product = activeProduct();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(creditProductRepository.findById(10L)).thenReturn(Optional.of(product));

        String orderNo = creditService.createOrder(1L, 10L);

        ArgumentCaptor<CreditOrder> orderCaptor = ArgumentCaptor.forClass(CreditOrder.class);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(creditOrderRepository).save(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());

        CreditOrder order = orderCaptor.getValue();
        Payment payment = paymentCaptor.getValue();
        assertThat(orderNo).startsWith("ORD-");
        assertThat(order.getOrderNo()).isEqualTo(orderNo);
        assertThat(order.getMember()).isSameAs(user);
        assertThat(order.getCreditProduct()).isSameAs(product);
        assertThat(order.getProductCodeSnapshot()).isEqualTo("CREDIT_100");
        assertThat(order.getProductNameSnapshot()).isEqualTo("100 크레딧");
        assertThat(order.getPriceAmountSnapshot()).isEqualTo(1000);
        assertThat(order.getTotalCreditSnapshot()).isEqualTo(110);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.READY);

        assertThat(payment.getCreditOrder()).isSameAs(order);
        assertThat(payment.getPgProvider()).isEqualTo(PgProvider.HECTO);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getRequestedAmount()).isEqualTo(1000);
    }

    @Test
    void rejectsInactiveProductBeforeCreatingOrder() {
        User user = User.builder().id(1L).build();
        CreditProduct product = activeProduct();
        product.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(creditProductRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> creditService.createOrder(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("비활성 상품입니다.");

        verify(creditOrderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsPaymentAccessByDifferentMember() {
        CreditOrder order = CreditOrder.builder()
                .orderNo("ORD-123")
                .member(User.builder().id(2L).build())
                .orderStatus(OrderStatus.READY)
                .build();
        when(creditOrderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> creditService.getOrderForPayment("ORD-123", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("접근 권한이 없습니다.");
    }

    @Test
    void rejectsAlreadyProcessedOrderBeforePayment() {
        CreditOrder order = CreditOrder.builder()
                .orderNo("ORD-123")
                .member(User.builder().id(1L).build())
                .orderStatus(OrderStatus.PAID)
                .build();
        when(creditOrderRepository.findByOrderNo("ORD-123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> creditService.getOrderForPayment("ORD-123", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 주문입니다.");
    }

    private CreditProduct activeProduct() {
        return CreditProduct.builder()
                .id(10L)
                .productCode("CREDIT_100")
                .productName("100 크레딧")
                .priceAmount(1000)
                .baseCreditAmount(100)
                .bonusCreditAmount(10)
                .totalCreditAmount(110)
                .isActive(true)
                .sortOrder(1)
                .build();
    }
}
