package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/region")
public class RegionController {

    @GetMapping("/selector")
    public String regionSelectorPage(Model model) {
        return "pages/region";
    }
}
