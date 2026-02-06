package com.example.bot.Telegram_bot_take_it.admin.service;

import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для корректной остановки Telegram-бота при завершении работы приложения.
 * <p>
 * Основная задача — отключить listener получения обновлений (getUpdates),
 * чтобы приложение завершалось чисто и бот не оставался в "подвешенном" состоянии.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BotUpdateService {

    /** Экземпляр TelegramBot (pengrad), через который управляем listener'ом обновлений */
    private final TelegramBot telegramBot;

    /**
     * Флаг состояния бота: запущен ли listener.
     * (В этом классе флаг только проверяется/сбрасывается при остановке)
     */
    @Getter
    private boolean isRunning = false;

    /**
     * Останавливает бота при уничтожении Spring-контекста (shutdown приложения).
     * <p>
     * Логика:
     *  - если бот не "запущен" (isRunning=false) — ничего не делает
     *  - снимает getUpdatesListener
     *  - ставит isRunning=false
     *  - делает небольшую паузу (sleep), чтобы операции корректно завершились
     */
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
