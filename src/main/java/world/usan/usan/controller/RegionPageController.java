package world.usan.usan.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/region")
public class RegionPageController {

    @GetMapping("/selector")
    public String regionSelectorPage(HttpSession session, Model model) {

        boolean tempLogin = Boolean.TRUE.equals(session.getAttribute("tempLogin"));
        model.addAttribute("tempLogin", tempLogin);
        model.addAttribute("step", 5);

        return "pages/region";
    }
}
