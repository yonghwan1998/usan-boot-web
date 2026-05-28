package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditApiController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;

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
        return ResponseEntity.ok(Map.of("orderNo", orderNo));
    }

}
