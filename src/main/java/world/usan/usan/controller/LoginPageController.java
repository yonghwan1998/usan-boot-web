package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/login")
@Controller
public class LoginPageController {

    @GetMapping("")
    public String loginPage() {
        return "pages/login";
    }
}
