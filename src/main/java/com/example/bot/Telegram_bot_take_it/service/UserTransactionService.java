package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTransactionService {

    private final UserRepository userRepository;

    /**
     * Регистрация или обновление пользователя (транзакционный)
     */
    @Transactional
    public User registerOrUpdateUser(TelegramUserDto userDto, Long chatId) {
        try {
            String telegramId = String.valueOf(userDto.getId());
            String displayName = createDisplayName(userDto);

            return userRepository.findByTelegramId(telegramId)
                    .map(existingUser -> updateExistingUser(existingUser, displayName, chatId))
                    .orElseGet(() -> createNewUserFromDto(telegramId, displayName, chatId));

        } catch (Exception e) {
            log.error("Ошибка регистрации пользователя: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Обновляет данные существующего пользователя
     *
     * <p>Сравнивает текущие данные пользователя с новыми и обновляет при необходимости:</p>
     * <ul>
     *   <li>Отображаемое имя пользователя</li>
     *   <li>Chat ID в Telegram</li>
     * </ul>
     *
     * <p><b>Примечание:</b> Метод помечен как private, так как вызывается только внутри
     * транзакционных методов сервиса для избежания self-invocation</p>
     *
     * @param existingUser существующий пользователь
     * @param displayName новое отображаемое имя
     * @param chatId новый Chat ID
     * @return обновленный объект пользователя
     */
    private User updateExistingUser(User existingUser, String displayName, Long chatId) {
        boolean updated = updateUser(chatId, displayName, existingUser);

        if (updated) {
            existingUser = userRepository.save(existingUser);
            log.info("Пользователь обновлен: {} (ID: {})", displayName, existingUser.getId());
        }

        return existingUser;
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
     * Создает нового пользователя на основе данных DTO
     *
     * <p>Создает объект пользователя со следующими параметрами:</p>
     * <ul>
     *   <li>Telegram ID - уникальный идентификатор пользователя в Telegram</li>
     *   <li>Отображаемое имя</li>
     *   <li>Chat ID - идентификатор чата</li>
     *   <li>Активный статус - по умолчанию true</li>
     *   <li>Администраторский статус - по умолчанию false</li>
     * </ul>
     *
     * @param telegramId Telegram ID пользователя
     * @param displayName отображаемое имя пользователя
     * @param chatId Chat ID в Telegram
     * @return созданный объект пользователя
     */
    private User createNewUserFromDto(String telegramId, String displayName, Long chatId) {
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

    /**
     * Создает отображаемое имя пользователя на основе данных DTO
     *
     * <p>Приоритеты выбора имени:</p>
     * <ol>
     *   <li>Username в Telegram (если указан)</li>
     *   <li>Имя и фамилия (если указано имя)</li>
     *   <li>Генерируемое имя формата "User_{id}"</li>
     * </ol>
     *
     * @param userDto DTO с данными пользователя из Telegram
     * @return строку с отображаемым именем пользователя
     */
    private String createDisplayName(TelegramUserDto userDto) {
        if (userDto.getUsername() != null && !userDto.getUsername().isEmpty()) {
            return userDto.getUsername();
        } else if (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) {
            return userDto.getFirstName() +
                    (userDto.getLastName() != null ? " " + userDto.getLastName() : "");
        } else {
            return "User_" + userDto.getId();
        }
    }
}
