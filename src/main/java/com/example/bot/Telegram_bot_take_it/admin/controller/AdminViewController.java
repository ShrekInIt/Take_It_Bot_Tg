package com.example.bot.Telegram_bot_take_it.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminViewController {
    @GetMapping("/admin")
    public String adminHome() {
        return "index";
    }
}
