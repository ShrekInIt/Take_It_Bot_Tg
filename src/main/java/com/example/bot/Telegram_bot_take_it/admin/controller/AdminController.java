package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.dto.UserDTO;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;

    // Endpoint для проверки аутентификации (должен быть доступен без аутентификации)
    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            // Пользователь аутентифицирован
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", authentication.getName());

            response.put("authenticated", true);
            response.put("user", userInfo);

        } else {
            // Пользователь не аутентифицирован
            response.put("authenticated", false);
        }
        return ResponseEntity.ok(response);
    }

    // Endpoint для текущего пользователя
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

    // Статистика
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

        stats.put("newUsersToday", 0); // временное значение

        return ResponseEntity.ok(stats);
    }

    // Пользователи
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

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        List<User> users = userService.findAll();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok((User) userDTOs);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setTelegramId(user.getTelegramId());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
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

    // Категории
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    // Продукты
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productService.findAll();

        // Преобразуем продукты в Map для удобства
        List<Map<String, Object>> productList = new ArrayList<>();

        for (Product product : products) {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("price", product.getAmount());
            productMap.put("available", product.getAvailable());

            // Получаем категорию
            if (product.getCategoryId() != null) {
                Optional<Object> categoryOpt = categoryService.findById(product.getCategoryId());
                if (categoryOpt.isPresent()) {
                    Category category = (Category) categoryOpt.get();
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

    // Заказы
    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getAllOrders() {
        List<Order> orders = orderService.findAll();

        List<Map<String, Object>> orderList = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("createdAt", order.getCreatedAt());

            // Получаем пользователя
            if (order.getUserId() != null) {
                Optional<User> userOpt = userService.findById(order.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getName());
                    orderMap.put("user", userMap);
                }
            }

            orderList.add(orderMap);
        }

        return ResponseEntity.ok(orderList);
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<List<Order>> getRecentOrders() {
        return ResponseEntity.ok(orderService.findRecentOrders(10));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
