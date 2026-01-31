package com.example.bot.Telegram_bot_take_it.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private Long customerChatId;
    private String phoneNumber;
    private String address;
    private String comments;
    private Integer totalAmount;
    private String deliveryType;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDtoBot> items;
}
