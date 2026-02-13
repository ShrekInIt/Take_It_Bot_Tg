package com.example.bot.Telegram_bot_take_it.utils;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TelegramMessageSender {

    private final TelegramSendQueue sendQueue;

    /**
     * Отправить простое текстовое сообщение БЕЗ разметки
     */
    public void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId.toString(), text);
        sendQueue.enqueue(chatId, request);
    }

    /**
     * Отправить простое текстовое сообщение с разметкой
     */
    public void sendMessageHtml(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId.toString(), text);
        request.parseMode(ParseMode.Markdown);
        sendQueue.enqueue(chatId, request);
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

        sendQueue.enqueue(chatId, request);
    }

    /**
     * Отправить сообщение с Inline клавиатурой
     */
    public void sendMessageWithInlineKeyboardHtml(Long chatId, String text, InlineKeyboardMarkup keyboard, boolean parseMode) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .replyMarkup(keyboard);

        if (parseMode) {
            request.parseMode(ParseMode.HTML);
        }

        sendQueue.enqueue(chatId, request);
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

        sendQueue.enqueue(chatId, request);
    }

    /**
     * Отправить сообщение с Reply клавиатурой
     */
    public void sendMessageWithReplyKeyboardHtml(Long chatId, String text, ReplyKeyboardMarkup keyboard, boolean parseMode) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .replyMarkup(keyboard);

        if (parseMode) {
            request.parseMode(ParseMode.HTML);
        }

        sendQueue.enqueue(chatId, request);
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

        sendQueue.enqueue(chatId, editMessage);
    }

    /**
     * Изменить сообщение
     */
    public void sendEditMessageHtml(Long chatId, int messageId,String text, InlineKeyboardMarkup keyboard, boolean parseMode) {
        EditMessageText editMessage = new EditMessageText(chatId, messageId, text)
                .replyMarkup(keyboard);

        if (parseMode) {
            editMessage.parseMode(ParseMode.HTML);
        }

        sendQueue.enqueue(chatId, editMessage);
    }


    /**
     * Изменить сообщение
     */
    public void sendEditCaption(Long chatId, int messageId,String text, InlineKeyboardMarkup keyboard, boolean parseMode) {
        EditMessageCaption editCaption = new EditMessageCaption(chatId, messageId)
                .caption(text)
                .replyMarkup(keyboard);

        if (parseMode) {
            editCaption.parseMode(ParseMode.HTML);
        }

        sendQueue.enqueue(chatId, editCaption);
    }

    /**
     * Отправить фото по id
     */
    public CompletableFuture<SendResponse> sendPhotoByFileId(Long chatId, String fileId,
                                                                  String caption,
                                                                  InlineKeyboardMarkup keyboard, boolean parseMode) {
        SendPhoto sendPhoto = new SendPhoto(chatId.toString(), fileId)
                .caption(caption)
                .replyMarkup(keyboard);
        if (parseMode) sendPhoto.parseMode(ParseMode.HTML);

        return sendQueue.enqueue(chatId, sendPhoto);
    }

    /**
     * Отправить фото
     */
    public CompletableFuture<SendResponse> sendPhoto(Long chatId, byte[] photoBytes,
                                                          String caption,
                                                          InlineKeyboardMarkup keyboard, boolean parseMode) {
        SendPhoto sendPhoto = new SendPhoto(chatId.toString(), photoBytes)
                .caption(caption)
                .replyMarkup(keyboard);
        if (parseMode) sendPhoto.parseMode(ParseMode.HTML);

        return sendQueue.enqueue(chatId, sendPhoto);
    }

    /**
     * Отправить несколько фото
     */
    public void sendMediaGroup(Long chatId, InputMediaPhoto[] mediaGroup){
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId.toString(), mediaGroup);
        sendQueue.enqueue(chatId, sendMediaGroup);
    }

    public void editOrSendMarkdown(Long chatId,
                                   Integer messageId,
                                   String text,
                                   InlineKeyboardMarkup keyboard) {

        if (messageId == null || messageId == 0) {
            sendMessageWithInlineKeyboard(chatId, text, keyboard, true);
            return;
        }

        EditMessageText edit = new EditMessageText(chatId, messageId, text)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        BaseResponse resp = sendQueue.enqueue(chatId, edit).join();


        if (resp == null || !resp.isOk()) {
            sendMessageWithInlineKeyboard(chatId, text, keyboard, true);
        }
    }
}
