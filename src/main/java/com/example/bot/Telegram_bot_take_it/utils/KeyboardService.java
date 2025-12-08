package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyboardService {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Создать клавиатуру с категориями для указанного родителя
     * Кэширует результат для оптимизации
     */
    @Cacheable(value = "categoryKeyboards", key = "#parentId")
    public InlineKeyboardMarkup getCategoryKeyboard(Long parentId) {
        var categories = (parentId == null)
                ? categoryService.getActiveRootCategories()
                : categoryService.getActiveSubcategories(parentId);

        if (categories.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        for (int i = 0; i < categories.size(); i += 2) {
            var cat1 = categories.get(i);
            InlineKeyboardButton button1 = new InlineKeyboardButton(cat1.getName())
                    .callbackData("category_" + cat1.getId());

            if (i + 1 < categories.size()) {
                var cat2 = categories.get(i + 1);
                InlineKeyboardButton button2 = new InlineKeyboardButton(cat2.getName())
                        .callbackData("category_" + cat2.getId());
                keyboardMarkup.addRow(button1, button2);
            } else {
                keyboardMarkup.addRow(button1);
            }
        }

        // Добавляем кнопку "Назад" если не корневой уровень
        if (parentId != null) {
            var currentCategory = categoryService.getCategoryById(parentId);
            method(keyboardMarkup, currentCategory);
        }

        return keyboardMarkup;
    }

    /**
     * Метод добавляет кнопку "Назад" в Inline-клавиатуру в зависимости от текущей позиции в иерархии категорий
     */
    public static void method(InlineKeyboardMarkup keyboardMarkup, Category currentCategory) {
        if (currentCategory != null && currentCategory.getParentId() != null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("category_" + currentCategory.getParentId());
            keyboardMarkup.addRow(backButton);
        } else if (currentCategory != null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Главное меню")
                    .callbackData("category_null");
            keyboardMarkup.addRow(backButton);
        }
    }

    /**
     * Создать клавиатуру с товарами для указанной категории
     */
    public InlineKeyboardMarkup getProductsWithQuantityKeyboard(Long categoryId) {
        var products = productService.getAvailableProductsWithStock(categoryId);

        if (products.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (var product : products) {
            // Проверяем количество
            if (product.getCount() == null || product.getCount() <= 0) {
                continue; // Пропускаем товары без остатка
            }

            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId());

            keyboard.addRow(button);
        }

        // Добавляем кнопку "Назад"
        var category = categoryService.getCategoryById(categoryId);
        if (category != null && category.getParentId() != null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("category_" + category.getParentId());
            keyboard.addRow(backButton);
        }

        return keyboard;
    }

    /**
     * Создать основную Reply-клавиатуру приложения
     * Содержит основные команды: меню, корзина, заказы, настройки
     */
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

