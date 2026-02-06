package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.controller.commands.CommandDispatcher;
import com.example.bot.Telegram_bot_take_it.handlers.OrderHandler;
import com.example.bot.Telegram_bot_take_it.service.TelegramUserRegistrar;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.message.InaccessibleMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class BotController {
    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final CallbackHandlerController callbackHandler;
    private final OrderHandler orderHandler;
    private final TelegramUserRegistrar telegramUserRegistrar;
    private final CommandDispatcher commandDispatcher;

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
     * Обрабатывает входящее сообщение пользователя:
     * - регистрирует пользователя
     * - определяет команду
     * - передаёт управление нужному handler'у
     */
    private void handleMessage(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();
        com.pengrad.telegrambot.model.User telegramUser = message.from();
        Integer messageId = message.messageId();

        telegramUserRegistrar.touch(telegramUser, chatId);

        if (message.contact() != null) {
            logger.info("Получен контакт от chatId {}: {}", chatId, message.contact().phoneNumber());
            String phoneNumber = message.contact().phoneNumber();
            orderHandler.handleContact(chatId, phoneNumber, messageId);
            return;
        }

        if (text != null) {
            handleCommand(chatId, text, telegramUser, messageId);
        }
    }

    /**
     * Обрабатывает callback-запрос от inline-кнопок
     * и передаёт управление в CallbackHandlerController
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        User from = callbackQuery.from();

        com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage maybe = callbackQuery.maybeInaccessibleMessage();

        if (maybe instanceof Message msg) {
            telegramUserRegistrar.touch(from, msg.chat().id());
        } else if (maybe instanceof InaccessibleMessage im) {
            telegramUserRegistrar.touch(from, im.chat().id());
        }

        callbackHandler.handleCallbackQuery(callbackQuery);
    }

    /**
     * Обработка текстовых команд от пользователя
     */
    private void handleCommand(Long chatId, String text, com.pengrad.telegrambot.model.User telegramUser, Integer messageId) {
        String command = text.trim().toLowerCase();

        logger.info("Command received: {} from chatId: {}", command, chatId);

        boolean handled = commandDispatcher.dispatch(chatId, text, telegramUser);
        if (!handled) {
            orderHandler.handleTextMessage(chatId, text, messageId);
        }
    }
}