package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CallbackHandlerController {
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Основной обработчик callback-запросов от inline-кнопок
     */
    @SuppressWarnings("deprecation")
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        Message message = callbackQuery.message();
        if (message == null) {
            log.error("Message is null in callback query");
            return;
        }

        Long chatId = message.chat().id();
        String data = callbackQuery.data();
        Integer messageId = message.messageId();

        answerCallback(callbackQuery.id(), null);

        try {
            if (data.startsWith("category_")) {
                handleCategoryCallback(chatId, messageId, data);
            } else if (data.startsWith("product_")) {
                handleProductCallback(chatId, data);
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage(), e);
            answerCallback(callbackQuery.id(), "❌ Ошибка обработки запроса");
        }
    }

    /**
     * Обработка callback-запросов для категорий
     */
    private void handleCategoryCallback(Long chatId, Integer messageId, String data) {
        String categoryIdStr = data.substring("category_".length());

        if ("null".equals(categoryIdStr)) {
            showRootCategories(chatId, messageId);
            return;
        }

        try {
            Long categoryId = Long.parseLong(categoryIdStr);
            showCategory(chatId, messageId, categoryId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка в данных категории");
        }
    }

    /**
     * Обработка callback-запросов для товаров
     */
    private void handleProductCallback(Long chatId, String data) {
        String productIdStr = data.substring("product_".length());
        try {
            Long productId = Long.parseLong(productIdStr);
            handleProductSelection(chatId, productId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка в данных товара");
        }
    }

    /**
     * Обработка выбора товара пользователем
     */
    private void handleProductSelection(Long chatId, Long productId) {

    }

    /**
     * Показать корневые категории (главное меню категорий)
     */
    private void showRootCategories(Long chatId, Integer messageId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            sendMessage(chatId, "📂 Категории пока не добавлены");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeEditMessage(chatId, editMessage);
    }

    /**
     * Показать содержимое категории: подкатегории и/или товары
     */
    private void showCategory(Long chatId, Integer messageId, Long categoryId) {
        // Используем CategoryService для получения информации о категории
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            sendMessage(chatId, "❌ Категория не найдена");
            return;
        }

        // Проверяем активность категории
        if (!category.isActive()) {
            sendMessage(chatId, "📭 Эта категория временно недоступна");
            return;
        }

        // Используем сервисы для проверки наличия подкатегорий и продуктов
        boolean hasSubcategories = categoryService.hasActiveSubcategories(categoryId);
        boolean hasProducts = productService.hasAvailableProductsInCategory(categoryId);

        // Получаем подкатегории и продукты
        List<Category> subcategories = hasSubcategories ? categoryService.getActiveSubcategories(categoryId) : List.of();
        List<Product> products = hasProducts ? productService.getAvailableProductsWithStock(categoryId) : List.of();

        InlineKeyboardMarkup keyboard;
        String messageText;

        if (hasSubcategories && hasProducts) {
            keyboard = categoryService.createCombinedKeyboard(categoryId, subcategories, products);
            messageText = categoryService.getCategoryDescription(category, true, true);
        } else if (hasSubcategories) {
            keyboard = keyboardService.getCategoryKeyboard(categoryId);
            messageText = categoryService.getCategoryDescription(category, true, false);
        } else if (hasProducts) {
            keyboard = keyboardService.getProductsWithQuantityKeyboard(categoryId);
            messageText = categoryService.getCategoryDescription(category, false, true);
        } else {
            sendMessage(chatId, "📭 В этой категории пока нет доступных товаров");
            return;
        }

        if (keyboard == null) {
            sendMessage(chatId, "📭 В этой категории пока нет доступных товаров");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, messageText)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeEditMessage(chatId, editMessage);
    }

    /**
     * Выполнить редактирование сообщения с новой клавиатурой
     */
    private void executeEditMessage(Long chatId, EditMessageText editMessage) {
        try {
            bot.execute(editMessage);
        } catch (Exception e) {
            log.error("Error editing message for chat {}: {}", chatId, e.getMessage());
            sendMessage(chatId, "❌ Ошибка при обновлении сообщения");
        }
    }

    /**
     * Отправить текстовое сообщение пользователю
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.Markdown);

        try {
            bot.execute(message);
        } catch (Exception e) {
            log.error("Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Ответить на callback-запрос (подтвердить получение)
     */
    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);

        if (text != null && !text.isEmpty()) {
            answer.text(text).showAlert(true);
        } else {
            answer.text("").showAlert(false);
        }

        try {
            bot.execute(answer);
        } catch (Exception e) {
            log.error("Error answering callback: {}", e.getMessage());
        }
    }
}
