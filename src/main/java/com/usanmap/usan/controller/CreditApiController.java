package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditApiController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;

    @PostMapping("/orders")
    public ResponseEntity<Map<String, String>> createOrder(@RequestParam Long productId) {
        Long userId = securityUtils.currentUserIdOrThrow();
        String orderNo = creditService.createOrder(userId, productId);
        return ResponseEntity.ok(Map.of("orderNo", orderNo));
    }

    @PostMapping("/orders/{orderNo}/mock-confirm")
    public ResponseEntity<Map<String, String>> mockConfirm(@PathVariable String orderNo) {
        Long userId = securityUtils.currentUserIdOrThrow();
        creditService.mockConfirm(orderNo, userId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
