package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.dto.response.CategoryResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CategoryData {
    private CategoryResponseDto category;
    private List<CategoryResponseDto> subcategories;
    private boolean hasProducts;
    private List<ProductResponseDto> products;
}
