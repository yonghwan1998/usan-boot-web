package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;
    private final SecurityUtils securityUtils;

    @RequestMapping("/")
    public String index(Model model) {
        Long userId = securityUtils.currentUserId();
        if (userId != null) {
            model.addAttribute("nearbyBrokers", mainService.getNearbyBrokers(userId));
            model.addAttribute("listingCards", mainService.getListingCards(userId));
        }

        return "index";
    }

    @RequestMapping("/download/apk")
    public String downloadApk() {
        return "pages/download/apk";
    }
}
