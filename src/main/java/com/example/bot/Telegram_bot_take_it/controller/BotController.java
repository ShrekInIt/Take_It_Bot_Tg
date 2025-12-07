package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class BotController {
    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final KeyboardService keyboardService;
    private final TelegramBot bot;
    private final CallbackHandlerController callbackHandler;

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

    private void handleMessage(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        if (text != null) {
            handleCommand(chatId, text);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        callbackHandler.handleCallbackQuery(callbackQuery);
    }

    private void handleCommand(Long chatId, String text) {
        String command = text.trim().toLowerCase();

        logger.info("Command received: {} from chatId: {}", command, chatId);

        switch (command) {
            case "/start" -> handleStartCommand(chatId);
            case "/help" -> handleHelpCommand(chatId);
            case "/menu" -> handleMenuCommand(chatId);
            case Messages.MENU_LOWERCASE -> handleMenuCommandCategory(chatId);
        }
    }

    private void handleStartCommand(Long chatId) {
        String welcomeText = Messages.HELLO_TEXT;
        sendMessage(chatId, welcomeText);
    }

    private void handleHelpCommand(Long chatId) {
        String helpText = Messages.HELP_TEXT;

        sendMessage(chatId, helpText);
    }

    private void handleMenuCommand(Long chatId) {
        ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();

        SendMessage request = new SendMessage(chatId.toString(), "🍽️ *Главное меню*\n\nВыберите действие:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeRequest(request, chatId);
    }

    private void handleMenuCommandCategory(Long chatId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        SendMessage request = new SendMessage(chatId.toString(), "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeRequest(request, chatId);
    }


    private void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.HTML);

        responser(request, chatId);
    }

    private void executeRequest(SendMessage request, Long chatId) {
        responser(request, chatId);
    }

    private void responser(SendMessage request, Long chatId) {
        try {
            SendResponse response = bot.execute(request);

            if (response.isOk()) {
                logger.info("✅ Message sent to chatId: {}", chatId);
            } else {
                logger.error("❌ Send failed for chatId {}: {} - {}",
                        chatId, response.errorCode(), response.description());
            }
        } catch (Exception e) {
            logger.error("⚠️ Network error for chatId {}: {}", chatId, e.getMessage());
        }
    }
}
