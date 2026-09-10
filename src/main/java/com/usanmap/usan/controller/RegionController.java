package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.UserRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/region")
@RequiredArgsConstructor
public class RegionController {

    private final UserRegionService userRegionService;
    private final SecurityUtils securityUtils;

    @GetMapping("/selector")
    public String regionSelectorPage(Model model) {
        return "pages/region";
    }

    @PostMapping("/selector")
    public String saveRegion(
            @RequestParam String admCd,
            @RequestParam String sidoName,
            @RequestParam String sigunguName,
            @RequestParam String emdName,
            @RequestParam BigDecimal emdLat,
            @RequestParam BigDecimal emdLng
    ) {
        Long userId = securityUtils.currentUserIdOrThrow();

        userRegionService.saveOrUpdate(userId, admCd, sidoName, sigunguName, emdName, emdLat, emdLng);

        return "redirect:/map";
    }
}
