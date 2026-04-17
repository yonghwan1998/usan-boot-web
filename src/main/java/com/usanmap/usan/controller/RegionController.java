package com.usanmap.usan.controller;

import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.UserRegionRepository;
import com.usanmap.usan.security.SecurityUtils;
import jakarta.transaction.Transactional;
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

    private final UserRegionRepository userRegionRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/selector")
    public String regionSelectorPage(Model model) {
        return "pages/region";
    }

    @Transactional
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

        userRegionRepository.findByUserId(userId).ifPresentOrElse(
                existing -> existing.update(admCd, sidoName, sigunguName, emdName, emdLat, emdLng),
                () -> userRegionRepository.save(UserRegion.builder()
                        .userId(userId)
                        .admCd(admCd)
                        .sidoName(sidoName)
                        .sigunguName(sigunguName)
                        .emdName(emdName)
                        .emdLat(emdLat)
                        .emdLng(emdLng)
                        .build())
        );

        return "redirect:/map";
    }
}
