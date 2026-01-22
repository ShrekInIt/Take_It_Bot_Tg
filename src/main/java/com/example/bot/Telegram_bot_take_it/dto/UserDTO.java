package com.example.bot.Telegram_bot_take_it.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String name;
    private String telegramId;
    private Long chatId;
    private String phoneNumber;
    private Boolean isActive = true;
    private Boolean isAdmin = false;
    private LocalDateTime createdAt;
}
