package com.example.bot.Telegram_bot_take_it.config;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Telegram-бота.
 * <p>
 * Создаёт и регистрирует TelegramBot как Spring Bean,
 * используя токен бота из application.properties / application.yml.
 */
@Configuration
public class TelegramBotConfig {

    /**
     * Токен Telegram-бота, берётся из конфигурации приложения:
     * telegram.bot.token=...
     */
    @Value("${telegram.bot.token}")
    private String botToken;

    /**
     * Регистрирует TelegramBot как Spring Bean.
     * <p>
     * После этого TelegramBot можно внедрять в сервисы,
     * которые отправляют сообщения/обрабатывают обновления.
     *
     * @return экземпляр TelegramBot, созданный с использованием токена
     */
    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(botToken);
    }
}
