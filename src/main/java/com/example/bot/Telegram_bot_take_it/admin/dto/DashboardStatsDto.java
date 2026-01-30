package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    private Long totalUsers;
    private Integer newUsersToday;
    private Long activeOrders;
    private Long totalProducts;
    private Integer ordersToday;
    private Integer todayRevenue;
}
