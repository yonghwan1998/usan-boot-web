package com.usanmap.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @RequestMapping("")
    public String notificationPage() {
        return "pages/notification";
    }
}
