package com.example.bot.Telegram_bot_take_it.admin.service;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotUpdateService {
    private final TelegramBot telegramBot;

    @Getter
    private boolean isRunning = false;

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
}
