package world.usan.usan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MapController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    @RequestMapping("/map")
    public String map(Model model) {
        model.addAttribute("naverClientId", naverClientId);
        return "pages/map";
    }
}
