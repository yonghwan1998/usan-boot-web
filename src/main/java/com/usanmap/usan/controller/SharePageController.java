package com.usanmap.usan.controller;

import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.ListingRepository;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class SharePageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    private final ListingRepository listingRepository;
    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    @GetMapping("/l/{publicId}")
    public String shareListingPage(@PathVariable String publicId, Model model) {
        Listing listing = listingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));
        model.addAttribute("listing", listing);
        model.addAttribute("naverClientId", naverClientId);
        return "pages/share/listing";
    }

    @GetMapping("/b/{publicId}")
    public String shareBrokerPage(@PathVariable String publicId, Model model) {
        BrokerPropertyCount broker = brokerPropertyCountRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("중개사를 찾을 수 없습니다."));
        model.addAttribute("broker", broker);
        return "pages/share/broker";
    }
}
