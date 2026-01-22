package com.example.bot.Telegram_bot_take_it.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminUserRequest {
    private Long id;
    private String username;

    @JsonProperty("password")
    private String password;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
