package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Единая точка регистрации/обновления пользователя Telegram в системе.
 * Используется во всех местах (messages / callbacks / команды).
 */
@Service
@RequiredArgsConstructor
public class TelegramUserRegistrar {

    private final UserTransactionService userTransactionService;
    private final Cache<Long, Long> lastTouch =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .maximumSize(100_000)
                    .build();


    /**
     * Регистрирует или обновляет пользователя Telegram и возвращает доменного User.
     */
    public User registerOrUpdate(com.pengrad.telegrambot.model.User tgUser, Long chatId) {
        TelegramUserDto dto = TelegramUserDto.builder()
                .id(tgUser.id())
                .username(tgUser.username())
                .firstName(tgUser.firstName())
                .lastName(tgUser.lastName())
                .isBot(tgUser.isBot())
                .languageCode(tgUser.languageCode())
                .build();

        return userTransactionService.registerOrUpdateUser(dto, chatId);
    }

    /**
     * Регистрирует или обновляет пользователя Telegram по DTO.
     */
    public User registerOrUpdate(TelegramUserDto dto, Long chatId) {
        return userTransactionService.registerOrUpdateUser(dto, chatId);
    }


    /**
     * Обновляет пользователя, если он есть, без обязательного возврата User.
     */
    public void touch(com.pengrad.telegrambot.model.User tgUser, Long chatId) {
        Long prev = lastTouch.getIfPresent(chatId);
        long now = System.currentTimeMillis();

        if (prev != null && (now - prev) < 60_000) return;

        lastTouch.put(chatId, now);
        registerOrUpdate(tgUser, chatId);
    }
}
