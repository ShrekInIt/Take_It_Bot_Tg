package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для заказа с позициями.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long id;
    private String orderNumber;
    private String status;
    private String deliveryType;
    private Long totalAmount;
    private String phoneNumber;
    private String address;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponseDto> items;
}

