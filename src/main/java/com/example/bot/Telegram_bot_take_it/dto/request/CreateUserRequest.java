package com.example.bot.Telegram_bot_take_it.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания нового пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
    private String name;

    @Size(max = 50, message = "Telegram ID не может быть больше 50 символов")
    private String telegramId;

    private Long chatId;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Неверный формат номера телефона")
    private String phoneNumber;

    private Boolean isActive;

    private Boolean isAdmin;
}

