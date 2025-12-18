package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/region")
public class RegionPageController {

    @GetMapping("/selector")
    public String regionSelectorPage(Model model) {

        model.addAttribute("step", 5);

        return "pages/region";
    }
}
