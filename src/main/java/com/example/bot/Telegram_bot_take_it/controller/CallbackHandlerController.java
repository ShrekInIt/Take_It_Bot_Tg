package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.handlers.*;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CallbackHandlerController {

    private final MessageSender messageSender;
    private final CategoryHandler categoryHandler;
    private final ProductHandler productHandler;
    private final QuantityHandler quantityHandler;
    private final CartHandler cartHandler;
    private final AddonHandler addonHandler;
    private final OrderHandler orderHandler;
    private final OrderHistoryHandler orderHistoryHandler;
    private final KeyboardService keyboardService;
    private final TelegramMessageSender telegramMessageSender;

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
        String callbackId = callbackQuery.id();

        log.info("=== ПОЛУЧЕН CALLBACK ===");
        log.info("Chat ID: {}, Message ID: {}, Data: {}", chatId, messageId, data);


        messageSender.answerCallback(callbackId, null);

        try {
            if (data.startsWith("category_")) {
                log.info("Обработка категории...");
                categoryHandler.handleCategoryCallback(chatId, messageId, data);
            } else if (data.startsWith("product_")) {
                log.info("Обработка товара...");
                productHandler.handleProductCallback(chatId, data);
            } else if (data.startsWith("quantity_plus_") || data.startsWith("quantity_minus_")) {
                log.info("Обработка изменения количества...");
                quantityHandler.handleQuantityChange(chatId, messageId, callbackId, data);
            } else if (data.startsWith("cart_")) {
                cartHandler.handlerCartCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("addons_")) {
                log.info("Обработка выбора добавок...");
                addonHandler.handlerAddonCallback(chatId, messageId, data);
            } else if (data.startsWith("repeat_order_")) {
                orderHistoryHandler.handleRepeatOrder(chatId, callbackId, data);
            } else if (data.startsWith("order_")) {
                orderHandler.handlerCartCallback(chatId, callbackId, data, messageId);
            }else if (data.startsWith("main_menu")) {
                ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();
                telegramMessageSender.sendMessageWithReplyKeyboard(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:", keyboard);
            } else if (data.startsWith("privacy_accepted")) {
                ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();
                telegramMessageSender.sendMessageWithReplyKeyboard(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:", keyboard);
            }else {
                log.warn("Неизвестный callback: {}", data);
                messageSender.answerCallback(callbackId, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка обработки запроса");
        }
    }
}
