package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.handlers.*;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.message.InaccessibleMessage;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
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
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage maybe = callbackQuery.maybeInaccessibleMessage();

        Long chatId;
        Integer messageId;

        if (maybe instanceof Message message) {
            chatId = message.chat().id();
            messageId = message.messageId();

        } else if (maybe instanceof InaccessibleMessage im) {
            chatId = im.chat().id();
            messageId = im.messageId();

            log.warn("Callback from inaccessible message: chatId={}, messageId={}", chatId, messageId);

        } else {
            log.warn("Callback from inline message, no chatId. callbackId={}", callbackQuery.id());
            messageSender.answerCallback(callbackQuery.id(), "❌ Команда недоступна");
            return;
        }

        String data = callbackQuery.data();
        String callbackId = callbackQuery.id();

        log.info("=== ПОЛУЧЕН CALLBACK ===");
        log.info("Chat ID: {}, Message ID: {}, Data: {}", chatId, messageId, data);

        messageSender.answerCallback(callbackId, null);

        try {
            if (data.startsWith("category_")) {
                categoryHandler.handleCategoryCallback(chatId, messageId, data);

            } else if (data.startsWith("product_")) {
                productHandler.handleProductCallback(chatId, data);

            } else if (data.startsWith("quantity_plus_") || data.startsWith("quantity_minus_")) {
                quantityHandler.handleQuantityChange(chatId, messageId, callbackId, data);

            } else if (data.startsWith("cart_")) {
                cartHandler.handlerCartCallback(chatId, callbackId, data, messageId);

            } else if (data.startsWith("addons_")) {
                addonHandler.handlerAddonCallback(chatId, messageId, data);

            } else if (data.startsWith("repeat_order_")) {
                orderHistoryHandler.handleRepeatOrder(chatId, callbackId, data, messageId);

            } else if (data.startsWith("order_")) {
                orderHandler.handlerCartCallback(chatId, callbackId, data, messageId);

            } else if (data.startsWith("main_menu") || data.startsWith("privacy_accepted")) {
                telegramMessageSender.sendMessageWithReplyKeyboard(
                        chatId,
                        "🍽 *Главное меню*\n\nВыберите категорию:",
                        keyboardService.getMainMenuKeyboard(),
                        true
                );

            } else {
                log.warn("Неизвестный callback: {}", data);
                messageSender.answerCallback(callbackId, "❌ Неизвестная команда");
            }

        } catch (Exception e) {
            log.error("Error handling callback", e);
            messageSender.answerCallback(callbackId, "❌ Ошибка обработки запроса");
        }
    }
}
