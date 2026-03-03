package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для позиции заказа.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Long priceAtOrder;
    private List<OrderItemAddonResponseDto> addons;
}

