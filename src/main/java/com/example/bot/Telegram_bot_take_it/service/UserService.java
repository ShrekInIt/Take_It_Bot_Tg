package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import com.example.bot.Telegram_bot_take_it.utils.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRegistrationService userRegistrationService;

    /**
     * Найти пользователя по chatId
     */
    public Optional<User> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    /**
     * Регистрация или обновление пользователя (новый метод с DTO)
     */
    @Transactional
    public User registerOrUpdateUser(TelegramUserDto userDto, Long chatId) {
        return userRegistrationService.registerOrUpdateUser(userDto, chatId);
    }

    /**
     * Регистрация или обновление пользователя
     */
    public void registerOrUpdateUser(com.pengrad.telegrambot.model.User telegramUser, Long chatId) {
        TelegramUserDto userDto = convertToDto(telegramUser);
        userRegistrationService.registerOrUpdateUser(userDto, chatId);
    }

    private TelegramUserDto convertToDto(com.pengrad.telegrambot.model.User telegramUser) {
        return TelegramUserDto.builder()
                .id(telegramUser.id())
                .username(telegramUser.username())
                .firstName(telegramUser.firstName())
                .lastName(telegramUser.lastName())
                .isBot(telegramUser.isBot())
                .languageCode(telegramUser.languageCode())
                .build();
    }
}
