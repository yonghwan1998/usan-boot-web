package world.usan.usan.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    @RequestMapping("/")
    public String index(HttpSession session, Model model) {

        boolean tempLogin = Boolean.TRUE.equals(session.getAttribute("tempLogin"));
        model.addAttribute("tempLogin", tempLogin);

        if (tempLogin) {
            model.addAttribute("nearbyAddr", "오산시 원동의 중개사");
        }

        return "index";
    }

    @GetMapping("/temp/login")
    public String tempLogin(HttpSession session) {
        session.setAttribute("tempLogin", Boolean.TRUE);

        return "redirect:/";
    }

    @GetMapping("/temp/logout")
    public String tempLogout(HttpSession session) {
        session.removeAttribute("tempLogin");

        return "redirect:/";
    }
}
