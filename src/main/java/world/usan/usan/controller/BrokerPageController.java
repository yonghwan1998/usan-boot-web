package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import world.usan.usan.entity.CrawledBroker;
import world.usan.usan.service.CrawledBrokerService;

import java.util.UUID;

@Controller
@RequestMapping("/broker")
@RequiredArgsConstructor
public class BrokerPageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    private final CrawledBrokerService crawledBrokerService;

    @GetMapping("/{brokerCode}")
    public String brokerPage(@PathVariable UUID brokerCode, Model model) {

        // TODO(yongss): broker_property_count 테이블의 컬럼을 broker_info 테이블과 맞추기
        // TODO(yongss): broker_info 테이블에서 데이터 가져오는 게 아니라 broker_property_count 테이블에서 데이터 가져오기
        CrawledBroker broker = crawledBrokerService.getBroker(brokerCode);

        model.addAttribute("broker", broker);
        model.addAttribute("naverClientId", naverClientId);

        return "pages/broker";
    }
}
