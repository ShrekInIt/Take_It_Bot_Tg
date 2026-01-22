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
    private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);

    private final TelegramBot bot;
    private final BotController botController;


    @PostConstruct
    public void init() throws InterruptedException {
        logger.info("=== STARTING TELEGRAM BOT ===");

        Thread.sleep(2000);
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                try {
                    botController.handleUpdate(update);
                } catch (Exception e) {
                    logger.error("Error processing update", e);
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                logger.error("Telegram API error: {} - {}",
                        e.response().errorCode(), e.response().description());
            } else {
                logger.error("Network error", e);
            }
        });

        logger.info("✅ Telegram bot is ready and listening for messages!");
    }
}
