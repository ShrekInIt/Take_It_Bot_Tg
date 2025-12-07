package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyboardService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "categoryKeyboards", key = "#parentId")
    public InlineKeyboardMarkup getCategoryKeyboard(Long parentId) {
        List<Category> categories;

        if (parentId == null) {
            // Корневые категории
            categories = categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrder();
        } else {
            // Подкатегории
            categories = categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrder(parentId);
        }

        if (categories.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        for (int i = 0; i < categories.size(); i += 2) {
            Category cat1 = categories.get(i);
            InlineKeyboardButton button1 = new InlineKeyboardButton(cat1.getName())
                    .callbackData("category_" + cat1.getId());

            if (i + 1 < categories.size()) {
                Category cat2 = categories.get(i + 1);
                InlineKeyboardButton button2 = new InlineKeyboardButton(cat2.getName())
                        .callbackData("category_" + cat2.getId());
                keyboardMarkup.addRow(button1, button2);
            } else {
                keyboardMarkup.addRow(button1);
            }
        }

        if (parentId != null) {
            Category currentCategory = categoryRepository.findById(parentId).orElse(null);
            if (currentCategory != null && currentCategory.getParentId() != null) {
                InlineKeyboardButton backButton = new InlineKeyboardButton("◀️ Назад")
                        .callbackData("category_" + currentCategory.getParentId());
                keyboardMarkup.addRow(backButton);
            } else if (currentCategory != null) {
                InlineKeyboardButton backButton = new InlineKeyboardButton("🏠 Главное меню")
                        .callbackData("category_null");
                keyboardMarkup.addRow(backButton);
            }
        }

        return keyboardMarkup;
    }

    @Cacheable(value = "productKeyboards", key = "#categoryId")
    public InlineKeyboardMarkup getProductsKeyboard(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndAvailableTrue(categoryId);

        if (products.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Product product : products) {
            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId());

            keyboard.addRow(button);
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            InlineKeyboardButton backButton;
            if (category.getParentId() != null) {
                backButton = new InlineKeyboardButton("◀️ Назад")
                        .callbackData("category_" + category.getParentId());
            } else {
                backButton = new InlineKeyboardButton("◀️ Назад")
                        .callbackData("category_null");
            }
            keyboard.addRow(backButton);
        }

        return keyboard;
    }

    public ReplyKeyboardMarkup getMainMenuKeyboard() {
        KeyboardButton menuButton = new KeyboardButton(Messages.MENU);
        KeyboardButton cartButton = new KeyboardButton("🛒 Корзина");
        KeyboardButton ordersButton = new KeyboardButton("📦 Мои заказы");
        KeyboardButton settingsButton = new KeyboardButton("⚙️ Настройки");

        return new ReplyKeyboardMarkup(
                new KeyboardButton[]{menuButton, cartButton},
                new KeyboardButton[]{ordersButton, settingsButton}
        )
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true);
    }
}

