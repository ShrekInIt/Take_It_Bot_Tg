package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для ответа с данными пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String telegramId;
    private Long chatId;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
}

