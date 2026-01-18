package com.example.bot.Telegram_bot_take_it.utils;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramMessageSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramMessageSender.class);

    private final TelegramBot bot;

    /**
     * Отправить простое текстовое сообщение БЕЗ разметки
     */
    public void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId.toString(), text);
        executeRequest(request, chatId);
    }

    /**
     * Отправить сообщение с Inline клавиатурой
     */
    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard, boolean parseMode) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .replyMarkup(keyboard);

        if (parseMode) {
            request.parseMode(ParseMode.Markdown);
        }

        executeRequest(request, chatId);
    }

    /**
     * Отправить сообщение с Reply клавиатурой
     */
    public void sendMessageWithReplyKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard, boolean parseMode) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .replyMarkup(keyboard);

        if (parseMode) {
            request.parseMode(ParseMode.Markdown);
        }

        executeRequest(request, chatId);
    }

    /**
     * Изменить сообщение
     */
    public void sendEditMessage(Long chatId, int messageId,String text, InlineKeyboardMarkup keyboard, boolean parseMode) {
        EditMessageText editMessage = new EditMessageText(chatId, messageId, text)
                .replyMarkup(keyboard);

        if (parseMode) {
            editMessage.parseMode(ParseMode.Markdown);
        }

        bot.execute(editMessage);
    }

    /**
     * Выполнение запроса на отправку сообщения с обработкой ошибок
     */
    private void responser(Long chatId, SendMessage request) {
        try {
            SendResponse response = bot.execute(request);

            if (response.isOk()) {
                log.info("✅ Message sent to chatId: {}", chatId);
                log.debug("✅ Message text: {}", request.getParameters().get("text"));
            } else {
                log.error("❌ Send failed for chatId {}: {} - {}",
                        chatId, response.errorCode(), response.description());
                log.debug("❌ Failed message text: {}", request.getParameters().get("text"));
            }
        } catch (Exception e) {
            log.error("⚠️ Network error for chatId {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Обертка для выполнения запроса отправки сообщения
     */
    private void executeRequest(SendMessage request, Long chatId) {
        String text = (String) request.getParameters().get("text");
        String parseMode = null;

        Object parseModeObj = request.getParameters().get("parse_mode");
        if (parseModeObj instanceof ParseMode) {
            parseMode = parseModeObj.toString();
        } else if (parseModeObj instanceof String) {
            parseMode = (String) parseModeObj;
        }
        log.debug("Отправка сообщения: chatId={}, parseMode={}, textLength={}",
                chatId, parseMode, text != null ? text.length() : 0);
        responser(chatId, request);
    }
}
