package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
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

@Controller
@Slf4j
@RequiredArgsConstructor
public class CallbackHandlerController {
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

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
            answerCallback(callbackQuery.id(), "Ошибка обработки запроса");
        }
    }

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

    private void handleProductCallback(Long chatId, String data) {
        String productIdStr = data.substring("product_".length());
        try {
            Long productId = Long.parseLong(productIdStr);
            // Здесь будет логика добавления товара в корзину
            // Пока просто отправляем сообщение
            sendMessage(chatId, "✅ Товар добавлен в корзину!");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка в данных товара");
        }
    }

    private void showRootCategories(Long chatId, Integer messageId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            sendMessage(chatId, "📂 Категории пока не добавлены");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        try {
            bot.execute(editMessage);
        } catch (Exception e) {
            log.error("Error editing message: {}", e.getMessage());
        }
    }

    private void showCategory(Long chatId, Integer messageId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            sendMessage(chatId, "❌ Категория не найдена");
            return;
        }

        boolean hasSubcategories = !categoryRepository
                .findByParentIdAndIsActiveTrueOrderBySortOrder(categoryId).isEmpty();

        boolean hasProducts = !productRepository
                .findByCategoryIdAndAvailableTrue(categoryId).isEmpty();

        InlineKeyboardMarkup keyboard;
        String messageText;

        if (hasSubcategories && hasProducts) {
            keyboard = getCombinedKeyboard(categoryId);
            messageText = "☕ *" + category.getName() + "*\n\nВыберите подкатегорию или товар:";
        } else if (hasSubcategories) {
            keyboard = keyboardService.getCategoryKeyboard(categoryId);
            messageText = "☕ *" + category.getName() + "*\n\nВыберите подкатегорию:";
        } else if (hasProducts) {
            keyboard = keyboardService.getProductsKeyboard(categoryId);
            messageText = "🛒 *" + category.getName() + "*\n\nВыберите товар:";
        } else {
            sendMessage(chatId, "📭 В этой категории пока нет товаров");
            return;
        }

        if (keyboard == null) {
            sendMessage(chatId, "📭 В этой категории пока нет товаров");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, messageText)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(editMessage);
    }

    private InlineKeyboardMarkup getCombinedKeyboard(Long categoryId) {
        List<Category> subcategories = categoryRepository
                .findByParentIdAndIsActiveTrueOrderBySortOrder(categoryId);

        List<Product> products = productRepository
                .findByCategoryIdAndAvailableTrue(categoryId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Category subcategory : subcategories) {
            InlineKeyboardButton button = new InlineKeyboardButton("📁 " + subcategory.getName())
                    .callbackData("category_" + subcategory.getId());
            keyboard.addRow(button);
        }

        for (Product product : products) {
            String buttonText = String.format("🛒 %s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId());

            keyboard.addRow(button);
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("◀️ Назад")
                    .callbackData("category_" + category.getParentId());
            keyboard.addRow(backButton);

        }

        return keyboard;
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.Markdown);

        try {
            bot.execute(message);
        } catch (Exception e) {
            log.error("Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }

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
