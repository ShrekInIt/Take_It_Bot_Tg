package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", authentication.getName());

            response.put("authenticated", true);
            response.put("user", userInfo);

        } else {
            response.put("authenticated", false);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", authentication.getName());
            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.ok(new HashMap<>());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalUsers", userService.countActiveUsers());
        } catch (Exception e) {
            stats.put("totalUsers", 0);
        }

        try {
            stats.put("activeOrders", orderService.countActiveOrders());
        } catch (Exception e) {
            stats.put("activeOrders", 0);
        }

        try {
            stats.put("totalProducts", productService.countAvailableProducts());
        } catch (Exception e) {
            stats.put("totalProducts", 0);
        }

        try {
            stats.put("todayRevenue", orderService.getTodayRevenue());
        } catch (Exception e) {
            stats.put("todayRevenue", 0);
        }

        stats.put("newUsersToday", 0);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<User>> getRecentUsers() {
        return ResponseEntity.ok(userService.findRecentUsers(10));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        return ResponseEntity.ok(userService.save(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }
}
