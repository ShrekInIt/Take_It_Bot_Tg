package com.example.bot.Telegram_bot_take_it.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {
    @GetMapping("/admin/users")
    public String usersPage() {
        return "admin/users";
    }

    @GetMapping("/admin/dashboard")
    public String dashboardPage() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/categories")
    public String categoriesPage() {
        return "admin/categories";
    }

    @GetMapping("/admin/products")
    public String productsPage() {
        return "admin/products";
    }

    @GetMapping("/admin/orders")
    public String ordersPage() {
        return "admin/orders";
    }

    @GetMapping("/admin/addons")
    public String addonsPage() {
        return "admin/addons";
    }

    @GetMapping("/admin/admins")
    public String adminsPage() {
        return "admin/admins";
    }
}
