package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    @RequestMapping("/")
    public String index() {
        // TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
        return "redirect:/map";
//        return "index";
    }

    @RequestMapping("/index2")
    public String index2() {
        // TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
        return "index";
    }

    @RequestMapping("/download/apk")
    public String downloadApk() {
        return "pages/download/apk";
    }
}
