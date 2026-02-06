package com.example.bot.Telegram_bot_take_it.controller.commands;

import com.example.bot.Telegram_bot_take_it.service.HandlerCommandService;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Диспетчер команд: определяет команду по тексту и выполняет действие.
 */
@Component
@RequiredArgsConstructor
public class CommandDispatcher {
    private final HandlerCommandService handlerCommandService;
    private final KeyboardService keyboardService;
    private final TelegramMessageSender messageSender;

    private final Map<String, BotCommand> lookup = buildLookup();

    /** Выполняет команду, если распознана. Возвращает true если команда обработана. */
    public boolean dispatch(Long chatId, String text, com.pengrad.telegrambot.model.User tgUser) {
        if (text == null) return false;

        String normalized = normalize(text);
        BotCommand cmd = lookup.get(normalized);
        if (cmd == null) return false;

        switch (cmd) {
            case START -> handlerCommandService.handleStartCommand(chatId, tgUser);
            case HELP -> messageSender.sendMessage(chatId, Messages.HELP_TEXT);
            case MENU -> messageSender.sendMessageWithReplyKeyboard(
                    chatId,
                    "🍽️ *Главное меню*\n\nВыберите категорию:",
                    keyboardService.getMainMenuKeyboard(),
                    true
            );
            case PHOTO_MENU -> handlerCommandService.handlerPhotoMenu(chatId);
            case BASKET -> handlerCommandService.handleBasketCommand(chatId, tgUser);
            case MENU_LOWER -> handlerCommandService.handleMenuCommandCategory(chatId);
            case ORDERS -> handlerCommandService.getAllOrdersUser(chatId);
            case ABOUT -> handlerCommandService.handleInfoAboutUs(chatId);
        }
        return true;
    }

    /** Нормализация ввода пользователя для поиска команды */
    private String normalize(String text) {
        return text.trim().toLowerCase();
    }

    private Map<String, BotCommand> buildLookup() {
        Map<String, BotCommand> map = new HashMap<>();
        for (BotCommand c : BotCommand.values()) {
            for (String alias : c.aliases()) {
                map.put(alias.trim().toLowerCase(), c);
            }
        }
        return map;
    }
}
