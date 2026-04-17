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
//        Long userId = securityUtils.currentUserId();
//        if (userId != null) {
//            model.addAttribute("nearbyBrokers", mainService.getNearbyBrokers(userId));
//        }
        // TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
        return "redirect:/map";
//        return "index";
    }

    @RequestMapping("/index2")
    public String index2(Model model) {
        // TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
        Long userId = securityUtils.currentUserId();
        if (userId != null) {
            model.addAttribute("nearbyBrokers", mainService.getNearbyBrokers(userId));
        }
        return "index";
    }

    @RequestMapping("/download/apk")
    public String downloadApk() {
        return "pages/download/apk";
    }
}
