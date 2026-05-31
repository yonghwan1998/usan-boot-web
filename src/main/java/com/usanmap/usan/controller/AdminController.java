package com.usanmap.usan.controller;

import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CreditService creditService;
    private final SmsService smsService;

    @GetMapping("/bank-transfer")
    public String bankTransferList(Model model) {
        model.addAttribute("orders", creditService.getPendingBankOrders());
        return "pages/admin/admin-bank-transfer";
    }

    @PostMapping("/bank-transfer/{orderId}/approve")
    public String approveBankTransfer(@PathVariable Long orderId) {
        CreditService.BankTransferResult result = creditService.approveBankTransfer(orderId);
        smsService.sendBankTransferApproved(result.userPhone(), result.totalCredit());
        return "redirect:/admin/bank-transfer";
    }
}
