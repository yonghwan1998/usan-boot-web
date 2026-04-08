package com.usanmap.usan.controller;

import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;

    @GetMapping("/manage")
    public String paymentsPage() {
        return "pages/payments/payments-manage";
    }

    @GetMapping("/history")
    public String paymentsHistory(@RequestParam(defaultValue = "ALL") String status, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();

        PaymentStatus paymentStatus = switch (status) {
            case "SUCCESS" -> PaymentStatus.SUCCESS;
            case "FAILED"  -> PaymentStatus.FAILED;
            default        -> null;
        };

        model.addAttribute("payments", creditService.getPayments(userId, paymentStatus));
        model.addAttribute("balance", creditService.getBalance(userId));
        model.addAttribute("status", status);
        return "pages/payments/payments-history";
    }
}
