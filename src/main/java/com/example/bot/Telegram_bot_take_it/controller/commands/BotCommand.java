package com.example.bot.Telegram_bot_take_it.controller.commands;

import com.example.bot.Telegram_bot_take_it.utils.Messages;

import java.util.List;

/**
 * Команды бота и их алиасы (текстовые варианты).
 */
public enum BotCommand {
    START(List.of("/start")),
    HELP(List.of("/help")),
    MENU(List.of("/menu")),
    PHOTO_MENU(List.of("/photomenu")),
    BASKET(List.of("/basket", "🛒 корзина", "корзина")),
    MENU_LOWER(List.of("меню", Messages.MENU_LOWERCASE)),
    ORDERS(List.of("📦 мои заказы", "/orders")),
    ABOUT(List.of("ℹ️ о нас"));

    private final List<String> aliases;

    BotCommand(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> aliases() {
        return aliases;
    }
}
