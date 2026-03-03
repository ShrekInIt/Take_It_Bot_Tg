package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO со сводной статистикой для Dashboard админ-панели.
 * <p>
 * Используется для отображения виджетов/карточек статистики:
 * пользователи, заказы, товары, выручка за сегодня и т.д.
 */
@Getter
@Setter
@Builder
public class DashboardStatsDto {

    /** Общее количество активных пользователей */
    private Long totalUsers;

    /** Количество новых пользователей за сегодня */
    private Integer newUsersToday;

    /** Количество активных заказов */
    private Long activeOrders;

    /** Общее количество доступных товаров */
    private Long totalProducts;

    /** Количество заказов за сегодня */
    private Integer ordersToday;

    /** Выручка за сегодня */
    private Long todayRevenue;
}
