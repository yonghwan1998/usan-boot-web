package com.usanmap.usan.controller;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.service.HectoPaymentReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationAdminControllerTests {

    @Mock private HectoPaymentReconciliationService reconciliationService;
    @InjectMocks private PaymentReconciliationAdminController controller;

    @Test
    void delegatesExplicitRecoveryRequest() {
        LocalDate targetDate = LocalDate.of(2026, 10, 30);
        HectoPaymentReconciliationService.ReconciliationSummary summary =
                new HectoPaymentReconciliationService.ReconciliationSummary(
                        1L, targetDate, HectoPaymentProfiles.Environment.PRODUCTION,
                        3, 2, 1, 0, 0, true
                );
        when(reconciliationService.reconcile(
                targetDate, HectoPaymentProfiles.Environment.PRODUCTION, true
        )).thenReturn(summary);

        var response = controller.reconcile(
                targetDate, HectoPaymentProfiles.Environment.PRODUCTION, true
        );

        assertThat(response.getBody()).isEqualTo(summary);
        verify(reconciliationService).reconcile(
                targetDate, HectoPaymentProfiles.Environment.PRODUCTION, true
        );
    }
}
