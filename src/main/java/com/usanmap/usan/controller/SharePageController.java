package com.usanmap.usan.controller;

import com.usanmap.usan.service.ListingLinkVisitService;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class SharePageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    private final ListingLinkVisitService linkVisits;

    private final ListingRepository listingRepository;
    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    @GetMapping("/l/{publicId}")
    public String shareListingPage(@PathVariable String publicId,
                                   @RequestParam(name = "t", required = false) String token,
                                   HttpServletRequest request,
                                   HttpServletResponse response, Model model) {
        Listing listing = listingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("매물을 찾을 수 없습니다."));
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        if ("GET".equals(request.getMethod())) {
            try {
                linkVisits.record(listing.getId(), token);
            } catch (DataAccessException | org.springframework.transaction.TransactionException e) {
                // 집계 장애가 매물 조회를 막지 않도록 하며 토큰·개인정보는 로그에 남기지 않는다.
                log.warn("매물 링크 방문 기록 실패 - listingId: {}", listing.getId());
            }
        }
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
