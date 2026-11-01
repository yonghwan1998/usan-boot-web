package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.PaymentObservability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Slf4j
public class CreditApiController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;
    private final PaymentObservability paymentObservability;

    private static final Set<String> ALLOWED_TYPES = Set.of("ALL", "CHARGE", "USE", "CANCEL");

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(defaultValue = "ALL") String type) {
        if (!ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = securityUtils.currentUserIdOrThrow();
        return ResponseEntity.ok(creditService.getSummary(userId, type));
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, String>> createOrder(@RequestParam Long productId) {
        Long userId = securityUtils.currentUserIdOrThrow();
        String orderNo = creditService.createOrder(userId, productId);
        paymentObservability.recordPaymentOrderCreated("hecto");
        log.info("payment_order_created orderNo={} productId={} provider=HECTO", orderNo, productId);
        return ResponseEntity.ok(Map.of("orderNo", orderNo));
    }

}
