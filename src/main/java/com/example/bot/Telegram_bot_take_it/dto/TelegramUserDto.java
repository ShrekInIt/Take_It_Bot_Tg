package com.example.bot.Telegram_bot_take_it.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUserDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private Boolean isBot;
    private String languageCode;
}
