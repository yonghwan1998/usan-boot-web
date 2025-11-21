package world.usan.usan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@RequestMapping("/map")
@Controller
public class MapPageController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    @GetMapping("")
    public String mapPage(Model model) {
        model.addAttribute("naverClientId", naverClientId);
        return "pages/map";
    }
}
