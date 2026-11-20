package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.ListingSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/listings/send-history")
@RequiredArgsConstructor
public class ListingSendController {
    private final ListingSendService listingSendService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        model.addAttribute("batches", listingSendService.getBatches(userId, page));
        model.addAttribute("hasLegacy", listingSendService.hasLegacy(userId));
        model.addAttribute("listingLabels", listingSendService.getListingLabels(userId));
        return "pages/listings/send-history";
    }

    @GetMapping("/{batchId}")
    public String detail(@PathVariable Long batchId, @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "ALL") String filter, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        model.addAttribute("item", listingSendService.getBatch(userId, batchId));
        var recipients = listingSendService.getRecipients(userId, batchId, page, filter);
        model.addAttribute("recipients", recipients);
        model.addAttribute("brokerAddresses", listingSendService.getRecipientAddresses(userId, recipients.getContent()));
        model.addAttribute("listingLabels", listingSendService.getListingLabels(userId));
        model.addAttribute("filter", filter);
        return "pages/listings/send-history-detail";
    }

    @GetMapping("/legacy")
    public String legacy(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("recipients", listingSendService.getLegacy(securityUtils.currentUserIdOrThrow(), page));
        return "pages/listings/send-history-legacy";
    }
}
