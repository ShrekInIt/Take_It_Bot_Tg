package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.dto.CategoryData;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryHandler {

    private final KeyboardService keyboardService;
    private final CategoryService categoryService;
    private final MessageSender messageSender;
    private final CategoryTransactionService categoryTransactionService;

    /**
     * Обработка callback-запросов для категорий
     */
    public void handleCategoryCallback(Long chatId, Integer messageId, String data) {

        String categoryIdStr = data.substring("category_".length());
        if ("null".equals(categoryIdStr)) {
            showRootCategories(chatId, messageId);
            return;
        }

        try {
            Long categoryId = Long.parseLong(categoryIdStr);

            showCategory(chatId, messageId, categoryId);
        } catch (NumberFormatException e) {
            messageSender.sendMessage(chatId, "❌ Ошибка в данных категории");
        }
    }

    /**
     * Показать корневые категории (главное меню категорий)
     */
    public void showRootCategories(Long chatId, Integer messageId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            messageSender.sendMessage(chatId, "📂 Категории пока не добавлены");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        messageSender.executeEditMessage(chatId, editMessage);
    }

    /**
     * Показать содержимое категории: подкатегории и/или товары
     */
    protected void showCategory(Long chatId, Integer messageId, Long categoryId) {
        CategoryData data = categoryTransactionService.getCategoryData(categoryId);

        if (data == null || data.getCategory() == null) {
            log.warn("Category not found for id: {}", categoryId);
            messageSender.sendMessage(chatId, "❌ Категория не найдена");
            return;
        }

        Category category = data.getCategory();
        List<Category> subcategories = data.getSubcategories();
        boolean hasProducts = data.isHasProducts();
        List<Product> products = data.getProducts();

        log.info("Found category: name={}, active={}", category.getName(), category.isActive());

        if (!category.isActive()) {
            messageSender.sendMessage(chatId, "📭 Эта категория временно недоступна");
            return;
        }

        boolean hasSubcategories = !subcategories.isEmpty();

        String previousType = messageSender.getAndUpdateLastMessageType(chatId);
        boolean fromProduct = "product".equals(previousType);

        log.info("Subcategories found: {}, Products available: {}", hasSubcategories, hasProducts);

        if (hasSubcategories && hasProducts) {
            log.info("Path: Has both subcategories and products");
            InlineKeyboardMarkup keyboard = categoryService.createCombinedKeyboard(
                    categoryId, subcategories, products);
            String messageText = categoryService.getCategoryDescription(category, true, true);

            messageSender.smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else if (hasSubcategories) {
            log.info("Path: Has only subcategories");
            InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(categoryId);
            String messageText = categoryService.getCategoryDescription(category, true, false);

            messageSender.smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else if (hasProducts) {
            log.info("Path: Has only products");
            InlineKeyboardMarkup keyboard = keyboardService.getProductsWithQuantityKeyboard(categoryId);
            String messageText = categoryService.getCategoryDescription(category, false, true);

            messageSender.smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else {
            log.warn("Path: No subcategories or products found!");
            messageSender.sendMessage(chatId, "📭 В этой категории пока нет доступных товаров");
        }
    }
}
