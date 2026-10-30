package com.usanmap.usan.controller;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.HectoPaymentService;
import com.usanmap.usan.service.HectoPaymentNotificationService;
import com.usanmap.usan.service.SmsService;
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
    @Mock private SecurityUtils securityUtils;
    @Mock private SmsService smsService;

    private CreditsController creditsController;

    @BeforeEach
    void setUp() {
        creditsController = new CreditsController(
                creditService,
                hectoPaymentService,
                hectoPaymentNotificationService,
                securityUtils,
                smsService
        );
    }

    @Test
    void chargePageDoesNotRestrictCardPaymentByAccount() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);

        String view = creditsController.chargeCredits(model);

        assertThat(view).isEqualTo("pages/credits/credits-charge");
        assertThat(model).doesNotContainKey("isTestAccount");
        verify(securityUtils, never()).currentUserEmail();
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

        assertThat(creditsController.hectoNoti(result)).isEqualTo("OK");
        verify(hectoPaymentNotificationService).accept(result);
        verify(creditService, never()).confirmHectoCharge(result);
    }

    @Test
    void hectoNotiReturnsFailWhenNotificationCannotBePersisted() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable"))
                .when(hectoPaymentNotificationService).accept(result);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
    }

    @Test
    void hectoNotiRejectsInvalidHashWithoutConfirmingPayment() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(false);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
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
