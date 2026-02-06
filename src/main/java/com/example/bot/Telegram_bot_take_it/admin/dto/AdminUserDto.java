package com.example.bot.Telegram_bot_take_it.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO пользователя для админ-панели.
 * <p>
 * Используется для выдачи списка пользователей/деталей пользователя в админке.
 * @JsonFormat задаёт формат даты createdAt при сериализации в JSON.
 */
@Getter
@Setter
@Builder
public class AdminUserDto {

    /** ID пользователя */
    private Long id;

    /** Имя пользователя */
    private String name;

    /** Telegram ID (как строка, чтобы не потерять формат/длину) */
    private String telegramId;

    /** Chat ID пользователя */
    private Long chatId;

    /** Является ли пользователь администратором */
    private Boolean isAdmin;

    /** Активен ли пользователь (например, не заблокирован) */
    private Boolean isActive;

    /** Номер телефона пользователя */
    private String phoneNumber;

    /** Дата/время создания пользователя (формат задаётся аннотацией)*/
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
