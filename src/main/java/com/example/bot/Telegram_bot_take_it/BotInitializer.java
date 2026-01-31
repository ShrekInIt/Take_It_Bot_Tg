package com.example.bot.Telegram_bot_take_it;

import com.example.bot.Telegram_bot_take_it.controller.BotController;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotInitializer {
    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final TelegramBot bot;
    private final BotController botController;

    @PostConstruct
    public void init() {
        log.info("🚀 Telegram Bot STARTED");

        bot.setUpdatesListener(updates -> {
            for (var update : updates) {
                try {
                    botController.handleUpdate(update);
                } catch (Exception e) {
                    log.error("Error processing update {}", update.updateId(), e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("✅ Telegram bot is listening");
    }
}
