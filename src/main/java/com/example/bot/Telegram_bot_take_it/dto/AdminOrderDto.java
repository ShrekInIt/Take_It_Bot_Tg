package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AdminOrderDto {
    private Long id;
    private String orderNumber;
    private Integer totalAmount;
    private Order.OrderStatus status;
    private Order.DeliveryType deliveryType;
    private String userName;
    private String phoneNumber;
    private String address;
    private String comments;
    private LocalDateTime createdAt;
}
