package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.handlers.OrderHandler;
import com.example.bot.Telegram_bot_take_it.service.HandlerCommandService;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class BotController {
    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final CallbackHandlerController callbackHandler;
    private final HandlerCommandService handlerCommandService;
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramMessageSender messageSender;
    private final OrderHandler orderHandler;

    /**
     * Основной метод обработки входящих обновлений от Telegram
     * Распределяет обработку между сообщениями и callback-запросами
     */
    public void handleUpdate(Update update) {
        try {
            if (update.message() != null) {
                handleMessage(update.message());
            } else if (update.callbackQuery() != null) {
                handleCallbackQuery(update.callbackQuery());
            }
        } catch (Exception e) {
            logger.error("Error handling update: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка входящих текстовых сообщений
     */
    private void handleMessage(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();
        com.pengrad.telegrambot.model.User telegramUser = message.from();

        userService.registerOrUpdateUser(telegramUser, chatId);

        if (message.contact() != null) {
            logger.info("Получен контакт от chatId {}: {}", chatId, message.contact().phoneNumber());
            String phoneNumber = message.contact().phoneNumber();
            orderHandler.handleContact(chatId, phoneNumber);
            return;
        }

        if (text != null) {
            handleCommand(chatId, text, telegramUser);
        }
    }

    /**
     * Обработка callback-запросов от inline-кнопок
     */
    @SuppressWarnings("deprecation")
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        userService.registerOrUpdateUser(callbackQuery.from(), callbackQuery.message().chat().id());
        callbackHandler.handleCallbackQuery(callbackQuery);
    }

    /**
     * Обработка текстовых команд от пользователя
     */
    private void handleCommand(Long chatId, String text, com.pengrad.telegrambot.model.User telegramUser) {
        String command = text.trim().toLowerCase();

        logger.info("Command received: {} from chatId: {}", command, chatId);

        switch (command.toLowerCase()) {
            case "/start" -> handlerCommandService.handleStartCommand(chatId, telegramUser);
            case "/help" -> messageSender.sendMessage(chatId, Messages.HELP_TEXT);
            case "/menu" -> messageSender.sendMessageWithReplyKeyboard(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:", keyboardService.getMainMenuKeyboard(), true);
            case "/photomenu" -> handlerCommandService.handlerPhotoMenu(chatId);
            case "/basket", "🛒 корзина", "корзина" ->handlerCommandService.handleBasketCommand(chatId, telegramUser);
            case Messages.MENU_LOWERCASE ->  handlerCommandService.handleMenuCommandCategory(chatId);
            case "\uD83D\uDCE6 мои заказы", "/orders" -> handlerCommandService.getAllOrdersUser(chatId);
            default -> orderHandler.handleTextMessage(chatId, text);
        }
    }
}