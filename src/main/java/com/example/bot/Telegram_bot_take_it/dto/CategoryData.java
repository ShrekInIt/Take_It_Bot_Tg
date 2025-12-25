package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CategoryData {
    private Category category;
    private List<Category> subcategories;
    private boolean hasProducts;
    private List<Product> products;
}
