package com.usanmap.usan.controller;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.HectoPaymentService;
import com.usanmap.usan.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditsController {

    private static final String MOCK_PAYMENT_ACCOUNT = "test@naver.com";

    private final CreditService creditService;
    private final HectoPaymentService hectoPaymentService;
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

    @PostMapping("/hecto/pay")
    public String hectoPay(@RequestParam String orderNo, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        try {
            CreditOrder order = creditService.getOrderForPayment(orderNo, userId);
            model.addAttribute("actionUrl", hectoPaymentService.getPaymentActionUrl());
            model.addAttribute("params", hectoPaymentService.buildPaymentParams(order));
            return "pages/credits/hecto-bridge";
        } catch (Exception e) {
            String msg = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/credits/fail?message=" + msg;
        }
    }

    /** nextUrl: 결제창에서 결제 완료 후 사용자 브라우저가 돌아오는 지점 */
    @RequestMapping(value = "/hecto/return", method = {RequestMethod.GET, RequestMethod.POST})
    public String hectoReturn(@RequestParam Map<String, String> result) {
        String orderNo = result.get("mchtTrdNo");
        if (orderNo == null || !hectoPaymentService.verifyResultHash(result)) {
            String msg = URLEncoder.encode("결제 검증에 실패했습니다.", StandardCharsets.UTF_8);
            return "redirect:/credits/fail?message=" + msg;
        }

        if (hectoPaymentService.isSuccess(result)) {
            creditService.confirmHectoCharge(result);
            return "redirect:/credits/complete?orderNo=" + orderNo;
        }

        String reason = result.getOrDefault("outRsltMsg", "결제가 실패했습니다.");
        String msg = URLEncoder.encode(reason, StandardCharsets.UTF_8);
        return "redirect:/credits/fail?message=" + msg;
    }

    /** cancUrl: 결제창에서 사용자가 결제를 취소했을 때 돌아오는 지점 */
    @RequestMapping(value = "/hecto/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public String hectoCancel() {
        String msg = URLEncoder.encode("결제를 취소했습니다.", StandardCharsets.UTF_8);
        return "redirect:/credits/fail?message=" + msg;
    }

    /** notiUrl: 헥토파이낸셜 서버가 직접 호출하는 서버-서버 결과 통보(No 세션/쿠키) */
    @PostMapping("/hecto/noti")
    @ResponseBody
    public String hectoNoti(@RequestParam Map<String, String> result) {
        if (!hectoPaymentService.verifyResultHash(result)) {
            return "FAIL";
        }
        try {
            creditService.confirmHectoCharge(result);
            return "OK";
        } catch (Exception e) {
            return "FAIL";
        }
    }

    @GetMapping("/fail")
    public String creditsFail(@RequestParam(required = false) String message, Model model) {
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
