package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/login")
@Controller
public class LoginController {

    @GetMapping("")
    public String loginPage() {
        return "pages/login/login";
    }

    @GetMapping("/find-id")
    public String findIdPage() {
        return "pages/login/find-id";
    }

    @GetMapping("/find-pw")
    public String findPwPage() {
        return "pages/login/find-pw";
    }
}
