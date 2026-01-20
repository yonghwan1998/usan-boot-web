package world.usan.usan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/broker")
public class BrokerPageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    @GetMapping("/{brokerCode}")
    public String brokerPage(Model model) {
        model.addAttribute("naverClientId", naverClientId);

        return "pages/broker";
    }
}
