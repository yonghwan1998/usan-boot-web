package com.usanmap.usan.controller;

import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditsController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;

    @GetMapping("/history")
    public String creditsHistory(@RequestParam(defaultValue = "USE") LedgerType type, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        model.addAttribute("ledgers", creditService.getLedgers(userId, type));
        model.addAttribute("balance", creditService.getBalance(userId));
        model.addAttribute("type", type);
        return "pages/credits/credits-history";
    }

    @GetMapping("/charge")
    public String chargeCredits(Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        model.addAttribute("products", creditService.getActiveProducts());
        model.addAttribute("balance", creditService.getBalance(userId));
        return "pages/credits/credits-charge";
    }

    @GetMapping("/charging")
    public String chargingCredits(@RequestParam Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "pages/credits/credits-charging";
    }

    @GetMapping("/complete")
    public String completeCredits(@RequestParam String orderNo, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        var order = creditService.getCompletedOrder(orderNo, userId);
        model.addAttribute("chargedCredit", order.getTotalCreditSnapshot());
        model.addAttribute("balance", creditService.getBalance(userId));
        return "pages/credits/credits-complete";
    }
}
