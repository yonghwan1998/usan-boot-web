package com.usanmap.usan.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.client.TossPaymentsClient;
import com.usanmap.usan.config.TossProperties;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditsController {

    private final CreditService creditService;
    private final SecurityUtils securityUtils;
    private final TossProperties tossProperties;
    private final TossPaymentsClient tossPaymentsClient;
    private final ObjectMapper objectMapper;
    private final SmsService smsService;

    @Value("${usan.bank.name}")
    private String bankName;

    @Value("${usan.bank.number}")
    private String bankNumber;

    @Value("${usan.bank.holder}")
    private String bankHolder;

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
        model.addAttribute("isTestAccount", tossProperties.isTestAccount(securityUtils.currentUserEmail()));
        return "pages/credits/credits-charge";
    }

    @GetMapping("/bank-transfer")
    public String bankTransferPage(@RequestParam Long productId, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        CreditService.CheckoutInfo info = creditService.getCheckoutInfo(userId, productId);
        model.addAttribute("productId", productId);
        model.addAttribute("productName", info.productName());
        model.addAttribute("amount", info.amount());
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankNumber", bankNumber);
        model.addAttribute("bankHolder", bankHolder);
        return "pages/credits/credits-bank-transfer";
    }

    @PostMapping("/bank-transfer/request")
    public String bankTransferRequest(
            @RequestParam Long productId,
            @RequestParam String depositorName,
            Model model
    ) {
        Long userId = securityUtils.currentUserIdOrThrow();
        CreditService.BankTransferResult result = creditService.createBankTransferOrder(userId, productId, depositorName.trim());
        String bankInfo = bankName + " " + bankNumber + " (" + bankHolder + ")";
        smsService.sendBankTransferRequest(result.userPhone(), depositorName.trim(), result.productName(), result.amount(), bankInfo);
        return "redirect:/credits/bank-transfer/pending";
    }

    @GetMapping("/bank-transfer/pending")
    public String bankTransferPending() {
        return "pages/credits/credits-bank-pending";
    }

    @GetMapping("/checkout")
    public String checkoutCredits(@RequestParam Long productId, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        CreditService.CheckoutInfo info = creditService.getCheckoutInfo(userId, productId);
        model.addAttribute("productId", productId);
        model.addAttribute("productName", info.productName());
        model.addAttribute("amount", info.amount());
        model.addAttribute("customerKey", "u_" + userId);
        model.addAttribute("customerEmail", info.customerEmail());
        model.addAttribute("customerName", info.customerName());
        model.addAttribute("tossClientKey", tossProperties.resolveClientKey(securityUtils.currentUserEmail()));
        return "pages/credits/credits-checkout";
    }

    @GetMapping("/toss/success")
    public String tossSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam int amount,
            Model model
    ) {
        Long userId = securityUtils.currentUserIdOrThrow();
        Map<String, Object> tossResponse = tossPaymentsClient.confirmPayment(paymentKey, orderId, amount);

        String method = tossResponse.containsKey("method") ? String.valueOf(tossResponse.get("method")) : null;
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(tossResponse);
        } catch (JsonProcessingException e) {
            rawJson = "{}";
        }

        creditService.confirmTossCharge(orderId, userId, paymentKey, method, amount, rawJson);
        return "redirect:/credits/complete?orderNo=" + orderId;
    }

    @GetMapping("/toss/fail")
    public String tossFail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam(required = false) String orderId,
            Model model
    ) {
        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        return "pages/credits/credits-fail";
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
