package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для добавки в позиции заказа.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemAddonResponseDto {
    private Long id;
    private Long addonProductId;
    private String addonProductName;
    private Integer quantity;
    private Long priceAtOrder;
}

