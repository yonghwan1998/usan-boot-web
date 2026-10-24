package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.HectoPaymentService;
import com.usanmap.usan.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditsControllerTests {

    @Mock private CreditService creditService;
    @Mock private HectoPaymentService hectoPaymentService;
    @Mock private SecurityUtils securityUtils;
    @Mock private SmsService smsService;

    private CreditsController creditsController;

    @BeforeEach
    void setUp() {
        creditsController = new CreditsController(creditService, hectoPaymentService, securityUtils, smsService);
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
    void hectoNotiReturnsOkOnlyWhenPaymentIsConfirmed() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(true);
        when(creditService.confirmHectoCharge(result)).thenReturn(true);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("OK");
    }

    @Test
    void hectoNotiReturnsFailWhenPaymentCannotBeConfirmed() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(true);
        when(creditService.confirmHectoCharge(result)).thenReturn(false);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
    }

    @Test
    void hectoNotiRejectsInvalidHashWithoutConfirmingPayment() {
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123");
        when(hectoPaymentService.verifyNotificationHash(result)).thenReturn(false);

        assertThat(creditsController.hectoNoti(result)).isEqualTo("FAIL");
        verify(creditService, never()).confirmHectoCharge(result);
    }
}
