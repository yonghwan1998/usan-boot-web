package com.usanmap.usan.controller;

import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.SmsService;
import com.usanmap.usan.service.PaymentObservability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CreditService creditService;
    private final SmsService smsService;
    private final PaymentObservability paymentObservability;

    @GetMapping("/bank-transfer")
    public String bankTransferList(Model model) {
        model.addAttribute("orders", creditService.getPendingBankOrders());
        return "pages/admin/admin-bank-transfer";
    }

    @PostMapping("/bank-transfer/{orderId}/approve")
    public String approveBankTransfer(@PathVariable Long orderId) {
        CreditService.BankTransferResult result = creditService.approveBankTransfer(orderId);
        paymentObservability.recordPaymentStatusTransition("admin", "PENDING_BANK", "PAID");
        log.info("payment_status_changed orderId={} orderNo={} from=PENDING_BANK to=PAID source=admin",
                orderId, result.orderNo());
        smsService.sendBankTransferApproved(result.userPhone(), result.totalCredit());
        return "redirect:/admin/bank-transfer";
    }
}
