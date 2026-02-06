package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Единая точка регистрации/обновления пользователя Telegram в системе.
 * Используется во всех местах (messages / callbacks / команды).
 */
@Service
@RequiredArgsConstructor
public class TelegramUserRegistrar {

    private final UserTransactionService userTransactionService;

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
        registerOrUpdate(tgUser, chatId);
    }
}
