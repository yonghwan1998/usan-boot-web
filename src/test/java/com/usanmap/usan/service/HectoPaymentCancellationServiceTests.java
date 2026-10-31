package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentCancellationServiceTests {

    @Mock
    private HectoPaymentCancellationTransactionService transactionService;
    @Mock
    private HectoCancellationClient cancellationClient;
    @Mock
    private PaymentObservability observability;

    private HectoPaymentCancellationService service;
    private HectoPaymentProfiles.PaymentProfile profile;
    private HectoPaymentCancellationTransactionService.PreparedCancellation prepared;

    @BeforeEach
    void setUp() {
        service = new HectoPaymentCancellationService(
                transactionService, cancellationClient, observability, true, "Asia/Seoul"
        );
        profile = new HectoPaymentProfiles.PaymentProfile(
                HectoPaymentProfiles.Environment.TEST,
                "test-mid", "hash", "12345678901234567890123456789012", true, "payment-url"
        );
        prepared = new HectoPaymentCancellationTransactionService.PreparedCancellation(
                10L, profile, "CANCEL1", "ORIGINAL1", 1000, "고객 요청",
                LocalDateTime.of(2026, 10, 31, 12, 0), false
        );
        when(transactionService.prepare(any(), any(), any())).thenReturn(prepared);
    }

    @Test
    void completesLocalStateOnlyAfterVerifiedProviderSuccess() {
        HectoCancellationClient.CancellationResponse response = new HectoCancellationClient.CancellationResponse(
                true, "0021", "0000", "정상", "PG-CANCEL-1", "{}"
        );
        when(cancellationClient.cancel(any(), any())).thenReturn(response);

        HectoPaymentCancellationService.CancellationResult result = service.cancelFull(1L, "고객 요청");

        assertThat(result.status()).isEqualTo(PaymentCancellationStatus.SUCCEEDED);
        verify(transactionService).markProcessing(10L);
        verify(transactionService).complete(10L, response);
        verify(transactionService, never()).failAndRelease(any(), any());
    }

    @Test
    void restoresReservedCreditOnExplicitProviderFailure() {
        HectoCancellationClient.CancellationResponse response = new HectoCancellationClient.CancellationResponse(
                false, "0031", "9999", "취소 거절", "", "{}"
        );
        when(cancellationClient.cancel(any(), any())).thenReturn(response);

        HectoPaymentCancellationService.CancellationResult result = service.cancelFull(1L, "고객 요청");

        assertThat(result.status()).isEqualTo(PaymentCancellationStatus.FAILED);
        verify(transactionService).failAndRelease(10L, response);
        verify(transactionService, never()).complete(any(), any());
    }

    @Test
    void requiresReviewWithoutAutomaticRetryWhenProviderOutcomeIsUnknown() {
        when(cancellationClient.cancel(any(), any())).thenThrow(new IllegalStateException("timeout"));

        HectoPaymentCancellationService.CancellationResult result = service.cancelFull(1L, "고객 요청");

        assertThat(result.status()).isEqualTo(PaymentCancellationStatus.REVIEW_REQUIRED);
        verify(transactionService).requireReview(10L, "PG 취소 응답을 확정할 수 없습니다: IllegalStateException");
        verify(transactionService, never()).complete(any(), any());
        verify(transactionService, never()).failAndRelease(any(), any());
    }
}
