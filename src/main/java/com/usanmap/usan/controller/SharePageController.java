package com.usanmap.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.ListingRepository;

@Controller
@RequestMapping("/share")
@RequiredArgsConstructor
public class SharePageController {

    private final ListingRepository listingRepository;

    @GetMapping("/{id}")
    public String sharePage(@PathVariable Long id, Model model) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));
        model.addAttribute("listing", listing);
        return "pages/share/listing";
    }
}
