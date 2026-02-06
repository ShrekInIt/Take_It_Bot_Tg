package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminOrderDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.DashboardStatsDto;
import com.example.bot.Telegram_bot_take_it.admin.service.AdminOrderService;
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

/**
 * Контроллер для главной страницы админ-панели (Dashboard).
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Возвращает агрегированную статистику для виджетов дашборда,
 * а также списки последних заказов и пользователей.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    /**
     * Сервис для работы с пользователями (подсчёты пользователей, последние пользователи и т.д.)
     */
    private final UserService userService;

    /**
     * Сервис для работы с заказами со стороны админки (подсчёты заказов, выручка, последние заказы)
     */
    private final AdminOrderService adminOrderService;

    /**
     * Сервис для работы с товарами (подсчёт доступных товаров и т.д.)
     */
    private final ProductService productService;

    /**
     * Возвращает сводную статистику для дашборда.
     * <p>
     * Собирает данные из разных сервисов и формирует объект DashboardStatsDto:
     *  - общее количество активных пользователей
     *  - количество новых пользователей за сегодня
     *  - количество активных заказов
     *  - количество заказов за сегодня
     *  - количество доступных товаров
     *  - выручка за сегодня
     *
     * @return DTO со статистикой для отображения в админ-панели
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalUsers(userService.countActiveUsers())
                .newUsersToday(userService.countNewUsersToday())
                .activeOrders(adminOrderService.countActiveOrders())
                .ordersToday(adminOrderService.countOrdersToday())
                .totalProducts(productService.countAvailableProducts())
                .todayRevenue(adminOrderService.calculateTodayRevenue())
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Возвращает список последних (недавних) заказов для дашборда.
     * <p>
     * В данном контроллере запрашивается 10 последних заказов.
     *
     * @return список заказов в виде AdminOrderDto
     */
    @GetMapping("/orders/recent")
    public ResponseEntity<List<AdminOrderDto>> getRecentOrders() {
        List<AdminOrderDto> orders = adminOrderService.getRecentOrders(10);
        return ResponseEntity.ok(orders);
    }

    /**
     * Возвращает список последних (недавно зарегистрированных) пользователей для дашборда.
     * <p>
     * В данном контроллере запрашивается 10 последних пользователей.
     *
     * @return список пользователей в виде AdminUserDto
     */
    @GetMapping("/users/recent")
    public ResponseEntity<List<AdminUserDto>> getRecentUsers() {
        List<AdminUserDto> users = userService.findRecentUsersDto(10);
        return ResponseEntity.ok(users);
    }
}
