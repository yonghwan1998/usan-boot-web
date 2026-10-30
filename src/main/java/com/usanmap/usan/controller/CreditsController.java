package com.usanmap.usan.controller;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.HectoPaymentService;
import com.usanmap.usan.service.HectoPaymentNotificationService;
import com.usanmap.usan.service.SmsService;
import com.usanmap.usan.service.PaymentObservability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequestMapping("/credits")
@RequiredArgsConstructor
@Slf4j
public class CreditsController {

    private static final int PAYMENT_STATUS_MAX_POLL_COUNT = 60;
    private static final String TEST_PAYMENT_ACCOUNT = "test@naver.com";

    private final CreditService creditService;
    private final HectoPaymentService hectoPaymentService;
    private final HectoPaymentNotificationService hectoPaymentNotificationService;
    private final PaymentObservability paymentObservability;
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
        model.addAttribute("isTestAccount", isTestPaymentAccount());
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
        paymentObservability.recordPaymentOrderCreated("bank_transfer");
        log.info("payment_order_created orderNo={} productId={} provider=BANK_TRANSFER",
                result.orderNo(), productId);
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
        if (isTestPaymentAccount()) {
            return "redirect:/credits/test-checkout?productId=" + productId;
        }
        addCheckoutAttributes(userId, productId, false, model);
        return "pages/credits/credits-checkout";
    }

    @GetMapping("/test-checkout")
    public String testCheckoutCredits(@RequestParam Long productId, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        if (!isTestPaymentAccount()) {
            return "redirect:/credits/checkout?productId=" + productId;
        }
        addCheckoutAttributes(userId, productId, true, model);
        return "pages/credits/credits-checkout";
    }

    private void addCheckoutAttributes(Long userId, Long productId, boolean testPayment, Model model) {
        CreditService.CheckoutInfo info = creditService.getCheckoutInfo(userId, productId);
        model.addAttribute("productId", productId);
        model.addAttribute("productName", info.productName());
        model.addAttribute("amount", info.amount());
        model.addAttribute("customerEmail", info.customerEmail());
        model.addAttribute("customerName", info.customerName());
        model.addAttribute("testPayment", testPayment);
    }

    @PostMapping("/hecto/pay")
    public String hectoPay(@RequestParam String orderNo, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        try {
            CreditOrder order = creditService.getOrderForPayment(orderNo, userId);
            boolean testPayment = isTestPaymentAccount();
            model.addAttribute("actionUrl", hectoPaymentService.getPaymentActionUrl(testPayment));
            model.addAttribute("params", hectoPaymentService.buildPaymentParams(order));
            log.info("payment_checkout_requested orderNo={} provider=HECTO gateway={}",
                    orderNo, testPayment ? "test" : "production");
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
        if (orderNo == null || orderNo.isBlank()) {
            String msg = URLEncoder.encode("결제 검증에 실패했습니다.", StandardCharsets.UTF_8);
            return "redirect:/credits/fail?message=" + msg;
        }

        if (hectoPaymentService.isSuccess(result)) {
            log.info("payment_browser_return orderNo={} outcome=success_claimed", orderNo);
            String encodedOrderNo = URLEncoder.encode(orderNo, StandardCharsets.UTF_8);
            return "redirect:/credits/complete?orderNo=" + encodedOrderNo;
        }

        // 브라우저 리턴값만으로 결제 상태를 확정하지 않는다. 최종 상태는 서명 검증된 서버 노티가 반영한다.
        log.info("payment_browser_return orderNo={} outcome=failed resultCode={}",
                orderNo, result.get("outRsltCd"));
        String reason = result.getOrDefault("outRsltMsg", "결제가 실패했습니다.");
        String msg = URLEncoder.encode(reason, StandardCharsets.UTF_8);
        return "redirect:/credits/fail?message=" + msg;
    }

    /** cancUrl: 결제창에서 사용자가 결제를 취소했을 때 돌아오는 지점 */
    @RequestMapping(value = "/hecto/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public String hectoCancel() {
        // 결제창 닫기는 PG 거래 취소가 아니므로 CANCELED 상태로 저장하지 않는다.
        log.info("payment_checkout_closed outcome=user_canceled");
        String msg = URLEncoder.encode("결제를 취소했습니다.", StandardCharsets.UTF_8);
        return "redirect:/credits/fail?message=" + msg;
    }

    /** notiUrl: 헥토파이낸셜 서버가 직접 호출하는 서버-서버 결과 통보(No 세션/쿠키) */
    @PostMapping(
            value = "/hecto/noti",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseBody
    public String hectoNoti(@RequestParam Map<String, String> result) {
        if (!hectoPaymentService.verifyNotificationHash(result)) {
            paymentObservability.recordNotificationReceived("invalid_signature");
            log.warn("payment_notification_rejected reason=invalid_signature");
            return "FAIL";
        }
        try {
            HectoPaymentNotificationService.AcceptanceResult acceptance =
                    hectoPaymentNotificationService.accept(result);
            String outcome = acceptance.duplicate() ? "duplicate" : "accepted";
            paymentObservability.recordNotificationReceived(outcome);
            log.info("payment_notification_received notificationId={} orderNo={} transactionNo={} outcome={}",
                    acceptance.notificationId(), acceptance.orderNo(), acceptance.transactionNo(), outcome);
            return "OK";
        } catch (Exception e) {
            paymentObservability.recordNotificationReceived("persistence_error");
            log.error("payment_notification_persistence_failed orderNo={} transactionNo={} exceptionType={}",
                    result.get("mchtTrdNo"), result.get("trdNo"), e.getClass().getSimpleName(), e);
            return "FAIL";
        }
    }

    @GetMapping("/fail")
    public String creditsFail(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("errorMessage", message != null ? message : "결제 처리 중 오류가 발생했습니다.");
        return "pages/credits/credits-fail";
    }

    @GetMapping("/complete")
    public String completeCredits(
            @RequestParam String orderNo,
            @RequestParam(defaultValue = "0") int pollCount,
            Model model
    ) {
        Long userId = securityUtils.currentUserIdOrThrow();
        var order = creditService.getOrderForCompletion(orderNo, userId);
        if (order.getOrderStatus() == OrderStatus.READY) {
            model.addAttribute("orderNo", orderNo);
            model.addAttribute("pollCount", pollCount + 1);
            model.addAttribute("autoRefresh", pollCount < PAYMENT_STATUS_MAX_POLL_COUNT);
            return "pages/credits/credits-processing";
        }
        if (order.getOrderStatus() != OrderStatus.PAID) {
            String failureMessage = switch (order.getOrderStatus()) {
                case FAILED -> "결제가 실패했습니다.";
                case EXPIRED -> "결제 결과를 확인하지 못했습니다. 결제 내역을 확인해 주세요.";
                case CANCELED -> "결제가 취소되었습니다.";
                case PENDING_BANK -> "입금 확인이 아직 완료되지 않았습니다.";
                default -> "결제를 완료하지 못했습니다.";
            };
            String msg = URLEncoder.encode(failureMessage, StandardCharsets.UTF_8);
            return "redirect:/credits/fail?message=" + msg;
        }
        model.addAttribute("chargedCredit", order.getTotalCreditSnapshot());
        model.addAttribute("balance", creditService.getBalance(userId));
        return "pages/credits/credits-complete";
    }

    private boolean isTestPaymentAccount() {
        String email = securityUtils.currentUserEmail();
        return email != null && TEST_PAYMENT_ACCOUNT.equalsIgnoreCase(email);
    }
}
