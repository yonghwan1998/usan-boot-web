package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy")
public class PolicyPageController {

    @GetMapping("terms")
    public String termsPage(){
        return "pages/policy/terms";
    }

    @GetMapping("privacy")
    public String privacyPage(){
        return "pages/policy/privacy";
    }
    @GetMapping("refund")
    public String refundPage(){
        return "pages/policy/refund";
    }
}
