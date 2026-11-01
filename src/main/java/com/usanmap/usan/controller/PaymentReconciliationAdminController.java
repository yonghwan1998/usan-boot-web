package com.usanmap.usan.controller;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.service.HectoPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/api/payment-reconciliation")
@RequiredArgsConstructor
public class PaymentReconciliationAdminController {

    private final HectoPaymentReconciliationService reconciliationService;

    @PostMapping
    public ResponseEntity<HectoPaymentReconciliationService.ReconciliationSummary> reconcile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam HectoPaymentProfiles.Environment environment,
            @RequestParam(defaultValue = "false") boolean recover
    ) {
        return ResponseEntity.ok(reconciliationService.reconcile(targetDate, environment, recover));
    }
}
