package com.usanmap.usan.controller;

import com.usanmap.usan.service.ListingLinkVisitService;
import com.usanmap.usan.entity.ListingSendHistory;
import java.util.List;
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
    private final ListingLinkVisitService linkVisits;
    private final SecurityUtils securityUtils;

    @GetMapping
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        var batches = listingSendService.getBatches(userId, page);
        model.addAttribute("batches", batches);
        model.addAttribute("batchVisits", linkVisits.batches(userId,
                batches.stream().map(b -> b.batch().getId()).toList()));
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
        model.addAttribute("recipientVisits", linkVisits.recipients(userId,
                recipients.stream().map(ListingSendHistory::getId).toList()));
        model.addAttribute("batchVisits", linkVisits.batches(userId, List.of(batchId)));
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
