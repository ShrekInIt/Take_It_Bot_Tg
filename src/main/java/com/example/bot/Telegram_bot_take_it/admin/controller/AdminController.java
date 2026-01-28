package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import com.example.bot.Telegram_bot_take_it.admin.service.AdminUserService;
import com.example.bot.Telegram_bot_take_it.dto.AdminUserRequest;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
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

import java.util.*;

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
    private final AdminUserService adminUserService;

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

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productService.findAll();

        List<Map<String, Object>> productList = new ArrayList<>();

        for (Product product : products) {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("price", product.getAmount());
            productMap.put("available", product.getAvailable());

            // Получаем категорию
            if (product.getCategoryId() != null) {
                Optional<Category> categoryOpt = categoryService.findById(product.getCategoryId());
                if (categoryOpt.isPresent()) {
                    Category category = categoryOpt.get();
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("id", category.getId());
                    categoryMap.put("name", category.getName());
                    productMap.put("category", categoryMap);
                }
            }

            productList.add(productMap);
        }

        return ResponseEntity.ok(productList);
    }

    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody AdminUserRequest dto) {
        adminUserService.create(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    @GetMapping("/admins/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        return adminUserService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admins/search")
    public ResponseEntity<?> searchAdmin(@RequestParam String username) {
        return adminUserService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable Long id,
            @RequestBody AdminUserRequest dto
    ) {
        log.info("Обновление администратора ID: {}, данные: {}", id, dto);
        log.info("Пароль передан: {}", dto.getPassword() != null ? "Да" : "Нет");

        try {
            AdminUser updated = adminUserService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Ошибка обновления администратора", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
