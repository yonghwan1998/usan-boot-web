package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/credits")
public class CreditsController {

    @GetMapping("/history")
    public String creditsHistory() {
        return "pages/credits/credits-history";
    }

    @GetMapping("/charge")
    public String chargeCredits() {
        return "pages/credits/credits-charge";
    }

    @GetMapping("/charging")
    public String chargingCredits() {
        return "pages/credits/credits-charging";
    }

    @GetMapping("/complete")
    public String completeCredits() {
        return "pages/credits/credits-complete";
    }
}
