package com.usanmap.usan.controller;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.HectoPaymentService;
import com.usanmap.usan.service.HectoPaymentNotificationService;
import com.usanmap.usan.service.SmsService;
import com.usanmap.usan.service.PaymentObservability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditsControllerTests {

    @Mock private CreditService creditService;
    @Mock private HectoPaymentService hectoPaymentService;
    @Mock private HectoPaymentNotificationService hectoPaymentNotificationService;
    @Mock private HectoPaymentProfiles hectoPaymentProfiles;
    @Mock private PaymentObservability paymentObservability;
    @Mock private SecurityUtils securityUtils;
    @Mock private SmsService smsService;

    private CreditsController creditsController;

    @BeforeEach
    void setUp() {
        creditsController = new CreditsController(
                creditService,
                hectoPaymentService,
                hectoPaymentNotificationService,
                hectoPaymentProfiles,
                paymentObservability,
                securityUtils,
                smsService
        );
    }

    @Test
    void chargePageMarksTestPaymentAccount() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("test@naver.com");
        when(hectoPaymentProfiles.isTestAccount("test@naver.com")).thenReturn(true);

        String view = creditsController.chargeCredits(model);

        assertThat(view).isEqualTo("pages/credits/credits-charge");
        assertThat(model.get("isTestAccount")).isEqualTo(true);
    }

    @Test
    void chargePageMarksRegularAccountAsProductionPaymentAccount() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("member@example.com");
        when(hectoPaymentProfiles.isTestAccount("member@example.com")).thenReturn(false);

        String view = creditsController.chargeCredits(model);

        assertThat(view).isEqualTo("pages/credits/credits-charge");
        assertThat(model.get("isTestAccount")).isEqualTo(false);
    }

    @Test
    void standardCheckoutRedirectsTestAccountToTestCheckout() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("test@naver.com");
        when(hectoPaymentProfiles.isTestAccount("test@naver.com")).thenReturn(true);

        String view = creditsController.checkoutCredits(10L, model);

        assertThat(view).isEqualTo("redirect:/credits/test-checkout?productId=10");
        verify(creditService, never()).getCheckoutInfo(1L, 10L);
    }

    @Test
    void testCheckoutRejectsRegularAccount() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("member@example.com");
        when(hectoPaymentProfiles.isTestAccount("member@example.com")).thenReturn(false);

        String view = creditsController.testCheckoutCredits(10L, model);

        assertThat(view).isEqualTo("redirect:/credits/checkout?productId=10");
        verify(creditService, never()).getCheckoutInfo(1L, 10L);
    }

    @Test
    void testCheckoutRendersForTestAccount() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("test@naver.com");
        when(hectoPaymentProfiles.isTestAccount("test@naver.com")).thenReturn(true);
        when(creditService.getCheckoutInfo(1L, 10L)).thenReturn(
                new CreditService.CheckoutInfo("테스트 상품", 1000, "test@naver.com", "테스터", "01012345678")
        );

        String view = creditsController.testCheckoutCredits(10L, model);

        assertThat(view).isEqualTo("pages/credits/credits-checkout");
        assertThat(model.get("testPayment")).isEqualTo(true);
        assertThat(model.get("productName")).isEqualTo("테스트 상품");
    }

    @Test
    void hectoPayRoutesTestAccountToTestGateway() {
        ExtendedModelMap model = new ExtendedModelMap();
        CreditOrder order = CreditOrder.builder().orderNo("ORD-123").build();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(securityUtils.currentUserEmail()).thenReturn("test@naver.com");
        when(creditService.getOrderForPayment("ORD-123", 1L)).thenReturn(order);
        when(hectoPaymentService.buildPaymentRequest(order, "test@naver.com")).thenReturn(
                new HectoPaymentService.PaymentRequest(
                        HectoPaymentProfiles.Environment.TEST,
                        "https://test-payment.example.com",
                        Map.of("mchtTrdNo", "ORD-123")
                )
        );

        String view = creditsController.hectoPay("ORD-123", model);

        assertThat(view).isEqualTo("pages/credits/hecto-bridge");
        assertThat(model.get("actionUrl")).isEqualTo("https://test-payment.example.com");
        verify(hectoPaymentService).buildPaymentRequest(order, "test@naver.com");
    }

    @Test
    void hectoReturnDoesNotConfirmPaymentAndRedirectsToCompletePage() {
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0021",
                "trdAmt", "encrypted-amount"
        );
        when(hectoPaymentService.isSuccess(result)).thenReturn(true);

        String view = creditsController.hectoReturn(result);

        assertThat(view).isEqualTo("redirect:/credits/complete?orderNo=ORD-123");
        verify(creditService, never()).confirmHectoCharge(result);
        verify(hectoPaymentService, never()).verifyNotificationHash(result);
    }

    @Test
    void hectoReturnRedirectsFailureResultWithoutConfirmingPayment() {
        Map<String, String> result = Map.of(
                "mchtTrdNo", "ORD-123",
                "outStatCd", "0031",
                "outRsltMsg", "승인 거절"
        );
        when(hectoPaymentService.isSuccess(result)).thenReturn(false);

        String view = creditsController.hectoReturn(result);

        assertThat(view).startsWith("redirect:/credits/fail?message=");
        verify(creditService, never()).confirmHectoCharge(result);
        verify(hectoPaymentService, never()).verifyNotificationHash(result);
    }

    @Test
    void hectoNotiReturnsOkAfterNotificationIsPersisted() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(true);
        when(hectoPaymentNotificationService.accept(result)).thenReturn(
                new HectoPaymentNotificationService.AcceptanceResult(1L, "ORD-123", "TRD-123", false)
        );

        assertThat(creditsController.hectoNoti(result)).isEqualTo("OK");
        verify(hectoPaymentNotificationService).accept(result);
        verify(paymentObservability).recordNotificationReceived("accepted");
        verify(creditService, never()).confirmHectoCharge(result);
    }

    @Test
    void hectoNotiReturnsFailWhenNotificationCannotBePersisted() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable"))
                .when(hectoPaymentNotificationService).accept(result);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
        verify(paymentObservability).recordNotificationReceived("persistence_error");
    }

    @Test
    void hectoNotiRejectsInvalidHashWithoutConfirmingPayment() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(false);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
        verify(paymentObservability).recordNotificationReceived("invalid_signature");
        verify(hectoPaymentNotificationService, never()).accept(result);
    }

    @Test
    void completePageWaitsWhileNotificationIsPending() {
        ExtendedModelMap model = new ExtendedModelMap();
        CreditOrder order = CreditOrder.builder().orderStatus(OrderStatus.READY).build();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(creditService.getOrderForCompletion("ORD-123", 1L)).thenReturn(order);

        String view = creditsController.completeCredits("ORD-123", 0, model);

        assertThat(view).isEqualTo("pages/credits/credits-processing");
        assertThat(model.get("orderNo")).isEqualTo("ORD-123");
        assertThat(model.get("autoRefresh")).isEqualTo(true);
        verify(creditService, never()).getBalance(1L);
    }

    @Test
    void completePageShowsBalanceOnlyAfterNotificationIsProcessed() {
        ExtendedModelMap model = new ExtendedModelMap();
        CreditOrder order = CreditOrder.builder()
                .orderStatus(OrderStatus.PAID)
                .totalCreditSnapshot(110)
                .build();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(creditService.getOrderForCompletion("ORD-123", 1L)).thenReturn(order);
        when(creditService.getBalance(1L)).thenReturn(310);

        String view = creditsController.completeCredits("ORD-123", 0, model);

        assertThat(view).isEqualTo("pages/credits/credits-complete");
        assertThat(model.get("chargedCredit")).isEqualTo(110);
        assertThat(model.get("balance")).isEqualTo(310);
    }

    @Test
    void completePageStopsPollingAfterDelayLimit() {
        ExtendedModelMap model = new ExtendedModelMap();
        CreditOrder order = CreditOrder.builder().orderStatus(OrderStatus.READY).build();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(creditService.getOrderForCompletion("ORD-123", 1L)).thenReturn(order);

        String view = creditsController.completeCredits("ORD-123", 60, model);

        assertThat(view).isEqualTo("pages/credits/credits-processing");
        assertThat(model.get("autoRefresh")).isEqualTo(false);
    }

    @Test
    void completePageExplainsExpiredPaymentAsIncomplete() {
        ExtendedModelMap model = new ExtendedModelMap();
        CreditOrder order = CreditOrder.builder().orderStatus(OrderStatus.EXPIRED).build();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(creditService.getOrderForCompletion("ORD-123", 1L)).thenReturn(order);

        String view = creditsController.completeCredits("ORD-123", 0, model);

        assertThat(view).startsWith("redirect:/credits/fail?message=");
        verify(creditService, never()).getBalance(1L);
    }
}
