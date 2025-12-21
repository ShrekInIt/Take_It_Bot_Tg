package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;

    @Transactional
    public User registerOrUpdateUser(TelegramUserDto userDto, Long chatId) {
        try {
            String telegramId = String.valueOf(userDto.getId());
            String displayName = createDisplayName(userDto);

            return userRepository.findByTelegramId(telegramId)
                    .map(existingUser -> updateExistingUser(existingUser, displayName, chatId))
                    .orElseGet(() -> createNewUser(telegramId, displayName, chatId));

        } catch (Exception e) {
            log.error("Ошибка регистрации пользователя: {}", e.getMessage(), e);
            return null;
        }
    }

    private User updateExistingUser(User existingUser, String displayName, Long chatId) {
        boolean updated = false;

        if (!displayName.equals(existingUser.getName())) {
            existingUser.setName(displayName);
            updated = true;
        }

        if (!chatId.equals(existingUser.getChatId())) {
            existingUser.setChatId(chatId);
            updated = true;
        }

        if (updated) {
            existingUser = userRepository.save(existingUser);
            log.info("Пользователь обновлен: {} (ID: {})", displayName, existingUser.getId());
        }

        return existingUser;
    }

    private User createNewUser(String telegramId, String displayName, Long chatId) {
        User newUser = User.builder()
                .telegramId(telegramId)
                .name(displayName)
                .chatId(chatId)
                .isActive(true)
                .isAdmin(false)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Новый пользователь зарегистрирован: {} (ID: {})", displayName, savedUser.getId());
        return savedUser;
    }

    private String createDisplayName(TelegramUserDto userDto) {
        if (userDto.getUsername() != null && !userDto.getUsername().isEmpty()) {
            return userDto.getUsername();
        } else if (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) {
            return userDto.getFirstName() +
                    (userDto.getLastName() != null ? " " + userDto.getLastName() : "");
        }
        return "User_" + userDto.getId();
    }
}
