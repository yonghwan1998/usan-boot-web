package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/broker")
public class BrokerPageController {

    @GetMapping("/{brokerCode}")
    public String brokerPage() {
        return "pages/broker";
    }
}
