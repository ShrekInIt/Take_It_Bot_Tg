package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.handlers.*;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CallbackHandlerController {

    private final MessageSender messageSender;
    private final CategoryHandler categoryHandler;
    private final ProductHandler productHandler;
    private final QuantityHandler quantityHandler;
    private final CartHandler cartHandler;
    private final AddonHandler addonHandler;
    private final OrderHandler orderHandler;


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
        System.out.println(data);
        try {
            if (data.startsWith("category_")) {
                log.info("Обработка категории...");
                categoryHandler.handleCategoryCallback(chatId, messageId, data);
            }
            else if (data.startsWith("product_")) {
                log.info("Обработка товара...");
                productHandler.handleProductCallback(chatId, data);
            }
            else if (data.startsWith("quantity_plus_") || data.startsWith("quantity_minus_")) {
                log.info("Обработка изменения количества...");
                quantityHandler.handleQuantityChange(chatId, messageId, callbackId, data);
            } else if (data.startsWith("cart_")) {
                cartHandler.handlerCartCallback(chatId, callbackId, data);
            } else if (data.startsWith("addons_")) {
                log.info("Обработка выбора добавок...");
                addonHandler.handlerAddonCallback(chatId, messageId, data);
            } else if (data.startsWith("order_")) {
                orderHandler.handlerCartCallback(chatId, callbackId, data);
            } else {
                log.warn("Неизвестный callback: {}", data);
                messageSender.answerCallback(callbackId, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка обработки запроса");
        }
    }
}
