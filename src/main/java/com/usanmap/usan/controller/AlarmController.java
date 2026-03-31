package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/alarm")
public class AlarmController {

    @RequestMapping("")
    public String alarmPage() {
        return "pages/alarm";
    }
}
