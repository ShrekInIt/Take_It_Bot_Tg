package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для группы позиций корзины.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemGroupResponseDto {
    private ProductResponseDto product;
    private List<CartItemResponseDto> items;
    private int totalQuantity;
    private boolean isCoffee;
}

