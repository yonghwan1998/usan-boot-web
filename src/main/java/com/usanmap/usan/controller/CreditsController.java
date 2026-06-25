package com.usanmap.usan.controller;

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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditsController {

    private static final String MOCK_PAYMENT_ACCOUNT = "test@naver.com";

    private final CreditService creditService;
    private final SecurityUtils securityUtils;
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
        model.addAttribute("isTestAccount", MOCK_PAYMENT_ACCOUNT.equals(securityUtils.currentUserEmail()));
        return "pages/credits/credits-charge";
    }

    @GetMapping("/bank-transfer")
    public String bankTransferPage(@RequestParam Long productId, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        CreditService.CheckoutInfo info = creditService.getCheckoutInfo(userId, productId);
        model.addAttribute("productId", productId);
        model.addAttribute("productName", info.productName());
        model.addAttribute("amount", info.amount());
        model.addAttribute("userPhone", info.userPhone());
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankNumber", bankNumber);
        model.addAttribute("bankHolder", bankHolder);
        return "pages/credits/credits-bank-transfer";
    }

    @PostMapping("/bank-transfer/request")
    public String bankTransferRequest(
            @RequestParam Long productId,
            @RequestParam String depositorName,
            @RequestParam(required = false) String phone,
            Model model
    ) {
        Long userId = securityUtils.currentUserIdOrThrow();
        CreditService.BankTransferResult result = creditService.createBankTransferOrder(userId, productId, depositorName.trim(), phone);
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
        model.addAttribute("customerEmail", info.customerEmail());
        model.addAttribute("customerName", info.customerName());
        return "pages/credits/credits-checkout";
    }

    @PostMapping("/kcp/mock/pay")
    public String kcpMockPay(@RequestParam String orderNo) {
        Long userId = securityUtils.currentUserIdOrThrow();
        try {
            creditService.confirmKcpMockCharge(orderNo, userId);
            return "redirect:/credits/complete?orderNo=" + orderNo;
        } catch (Exception e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/credits/kcp/fail?message=" + msg;
        }
    }

    @GetMapping("/kcp/fail")
    public String kcpFail(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("errorMessage", message != null ? message : "결제 처리 중 오류가 발생했습니다.");
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
