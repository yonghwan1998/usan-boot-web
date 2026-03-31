package com.usanmap.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.usanmap.usan.dto.BrokerPropertyTagDto;
import com.usanmap.usan.entity.CrawledBroker;
import com.usanmap.usan.service.BrokerPropertyCountService;
import com.usanmap.usan.service.CrawledBrokerService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/broker")
@RequiredArgsConstructor
public class BrokerPageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    private final CrawledBrokerService crawledBrokerService;
    private final BrokerPropertyCountService brokerPropertyCountService;

    @GetMapping("/{brokerCode}")
    public String brokerPage(@PathVariable UUID brokerCode, Model model) {

        CrawledBroker broker = crawledBrokerService.getBroker(brokerCode);
        List<BrokerPropertyTagDto> tags = brokerPropertyCountService.getBrokerTagsAll(brokerCode);

        model.addAttribute("broker", broker);
        model.addAttribute("tags", tags);
        model.addAttribute("naverClientId", naverClientId);

        return "pages/broker";
    }
}
