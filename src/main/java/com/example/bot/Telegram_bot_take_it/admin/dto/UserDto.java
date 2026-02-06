package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Упрощённый DTO пользователя.
 * <p>
 * Используется как вложенный объект (например, внутри OrderDto),
 * чтобы отдавать только нужные поля пользователя (без лишних данных).
 */
@Getter
@Setter
public class UserDto {
    private Long id;

    private String username;

    private String telegramId;

    private String phoneNumber;
}
