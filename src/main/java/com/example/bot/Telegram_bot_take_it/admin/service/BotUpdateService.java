package com.example.bot.Telegram_bot_take_it.admin.service;

import com.example.bot.Telegram_bot_take_it.controller.BotController;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotUpdateService {
    private final TelegramBot telegramBot;
    private final BotController botController;

    @Getter
    private boolean isRunning = false;

    public void startBot() {
        if (isRunning) {
            log.warn("Bot is already running");
            return;
        }

        try {
            telegramBot.setUpdatesListener(updates -> {
                try {
                    return processUpdates(updates);
                } catch (Exception e) {
                    log.error("Error processing updates", e);
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                }
            }, e -> {
                if (e != null) {
                    log.error("Error in updates listener", e);
                }
            });

            isRunning = true;
            log.info("Telegram bot started successfully");
        } catch (Exception e) {
            log.error("Failed to start bot", e);
        }
    }

    @PreDestroy
    public void stopBot() {
        if (!isRunning) {
            return;
        }

        try {
            telegramBot.removeGetUpdatesListener();
            isRunning = false;
            Thread.sleep(2000);

            log.info("Telegram bot stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping bot", e);
        }
    }

    private int processUpdates(List<Update> updates) {
        if (updates == null || updates.isEmpty()) {
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }

        for (Update update : updates) {
            try {
                processUpdate(update);
            } catch (Exception e) {
                log.error("Error processing update: {}", update.updateId(), e);
            }
        }

        return updates.getLast().updateId();
    }

    private void processUpdate(Update update) {
        log.debug("Received update: {}", update.updateId());

        if (update.message() != null) {
            log.info("Message from {}: {}", update.message().chat().id(), update.message().text());
        }

        try {
            botController.handleUpdate(update);
            log.debug("Update {} forwarded to BotController", update.updateId());
        } catch (Exception e) {
            log.error("Error forwarding update to BotController", e);
        }
    }
}
