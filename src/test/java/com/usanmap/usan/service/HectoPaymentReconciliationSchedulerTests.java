package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HectoPaymentReconciliationSchedulerTests {

    @Mock private HectoPaymentReconciliationService reconciliationService;

    @Test
    void reconcilesTestAndProductionForLookbackDateInDryRunMode() {
        HectoPaymentReconciliationScheduler scheduler =
                new HectoPaymentReconciliationScheduler(reconciliationService);
        ReflectionTestUtils.setField(scheduler, "lookbackDays", 1);
        ReflectionTestUtils.setField(scheduler, "autoRecoverEnabled", false);
        ReflectionTestUtils.setField(scheduler, "zone", "Asia/Seoul");
        LocalDate expectedDate = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1);

        scheduler.reconcilePreviousTransactions();

        verify(reconciliationService).reconcile(expectedDate, HectoPaymentProfiles.Environment.TEST, false);
        verify(reconciliationService).reconcile(expectedDate, HectoPaymentProfiles.Environment.PRODUCTION, false);
    }
}
