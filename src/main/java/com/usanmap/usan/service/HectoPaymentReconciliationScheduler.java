package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hecto.reconciliation.enabled", havingValue = "true")
public class HectoPaymentReconciliationScheduler {

    private final HectoPaymentReconciliationService reconciliationService;

    @Value("${hecto.reconciliation.lookback-days:1}")
    private int lookbackDays;

    @Value("${hecto.reconciliation.auto-recover-enabled:false}")
    private boolean autoRecoverEnabled;

    @Value("${hecto.reconciliation.zone:Asia/Seoul}")
    private String zone;

    @Scheduled(
            cron = "${hecto.reconciliation.cron:0 0 3 * * *}",
            zone = "${hecto.reconciliation.zone:Asia/Seoul}"
    )
    public void reconcilePreviousTransactions() {
        LocalDate targetDate = LocalDate.now(ZoneId.of(zone)).minusDays(lookbackDays);
        for (HectoPaymentProfiles.Environment environment : HectoPaymentProfiles.Environment.values()) {
            try {
                reconciliationService.reconcile(targetDate, environment, autoRecoverEnabled);
            } catch (Exception e) {
                log.error("payment_reconciliation_schedule_failed targetDate={} environment={} exceptionType={}",
                        targetDate, environment, e.getClass().getSimpleName(), e);
            }
        }
    }
}
