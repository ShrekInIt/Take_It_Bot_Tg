package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminOrderDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.DashboardStatsDto;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalUsers(userService.countActiveUsers())
                .newUsersToday(userService.countNewUsersToday())
                .activeOrders(orderService.countActiveOrders())
                .ordersToday(orderService.countOrdersToday())
                .totalProducts(productService.countAvailableProducts())
                .todayRevenue(orderService.calculateTodayRevenue())
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<List<AdminOrderDto>> getRecentOrders() {
        List<AdminOrderDto> orders = orderService.getRecentOrders(10);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<AdminUserDto>> getRecentUsers() {
        List<AdminUserDto> users = userService.findRecentUsersDto(10);
        return ResponseEntity.ok(users);
    }
}
