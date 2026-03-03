package com.example.bot.Telegram_bot_take_it.mapper;

import com.example.bot.Telegram_bot_take_it.dto.request.CreateUserRequest;
import com.example.bot.Telegram_bot_take_it.dto.request.UpdateUserRequest;
import com.example.bot.Telegram_bot_take_it.dto.response.UserResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования User Entity в DTO и обратно
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserMapper {

    /**
     * Преобразует User Entity в UserResponseDto
     */
    public UserResponseDto toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .telegramId(user.getTelegramId())
                .chatId(user.getChatId())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .isAdmin(user.getIsAdmin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Создает новый User Entity из CreateUserRequest
     */
    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .name(request.getName())
                .telegramId(request.getTelegramId())
                .chatId(request.getChatId())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false)
                .build();
    }

    /**
     * Обновляет существующий User Entity из UpdateUserRequest
     * Обновляются только поля, которые присутствуют в request (не null)
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        if (user == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getTelegramId() != null) {
            user.setTelegramId(request.getTelegramId());
        }

        if (request.getChatId() != null) {
            user.setChatId(request.getChatId());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getIsAdmin() != null) {
            user.setIsAdmin(request.getIsAdmin());
        }
    }
}

