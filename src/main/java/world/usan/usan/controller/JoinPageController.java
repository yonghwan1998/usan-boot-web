package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/join")
@Controller
public class JoinPageController {

    @GetMapping("")
    public String mapPage(Model model) {
        model.addAttribute("joinStep", 1);
        return "pages/join";
    }
}
