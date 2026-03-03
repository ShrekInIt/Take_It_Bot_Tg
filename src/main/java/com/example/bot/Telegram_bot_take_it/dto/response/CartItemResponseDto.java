package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для позиции корзины.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {
    private Long id;
    private ProductResponseDto product;
    private Integer countProduct;
    private String specialInstructions;
    private List<CartItemAddonResponseDto> addons;
}

