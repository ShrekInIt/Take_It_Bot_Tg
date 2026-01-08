package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserTransactionService userTransactionService;

    /**
     * Найти пользователя по telegramId
     */
    public Optional<User> getUserByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    /**
     * Найти пользователя по chatId
     */
    public Optional<User> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    /**
     * Регистрация или обновление пользователя
     */
    public void registerOrUpdateUser(com.pengrad.telegrambot.model.User telegramUser, Long chatId) {
        try {
            String telegramId = String.valueOf(telegramUser.id());
            String username = telegramUser.username();
            String firstName = telegramUser.firstName();

            String displayName;
            if (username != null && !username.isEmpty()) {
                displayName = username;
            } else if (firstName != null && !firstName.isEmpty()) {
                displayName = firstName;
            } else {
                displayName = "User_" + telegramId;
            }

            getUserByTelegramId(telegramId)
                    .map(existingUser -> {
                        boolean updated = updateUser(chatId, displayName, existingUser);

                        if (updated) {
                            existingUser = userTransactionService.updateUser(existingUser);
                            logger.info("Пользователь обновлен: {} (ID: {})", displayName, existingUser.getId());
                        }

                        return existingUser;
                    })
                    .orElseGet(() -> {
                        User newUser = userTransactionService.createUser(telegramId, displayName, chatId);
                        logger.info("Новый пользователь зарегистрирован: {} (ID: {})", displayName, newUser.getId());
                        return newUser;
                    });

        } catch (Exception e) {
            logger.error("Ошибка регистрации пользователя: {}", e.getMessage());
        }
    }

    /**
     * Обновление пользователя
     */
    public boolean updateUser(Long chatId, String displayName, @NotNull User existingUser) {
        boolean updated = false;

        if (!displayName.equals(existingUser.getName())) {
            existingUser.setName(displayName);
            updated = true;
        }

        if (!chatId.equals(existingUser.getChatId())) {
            existingUser.setChatId(chatId);
            updated = true;
        }

        return updated;
    }

    /**
     * Обновить номер телефона пользователя
     */
    @Transactional
    public void updatePhoneNumber(Long chatId, String phoneNumber) {
        User user = getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
        log.info("Номер телефона обновлен для пользователя: {} ({})", user.getName(), phoneNumber);
    }
}
