package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Найти пользователя по ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

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
     * Найти или создать пользователя
     */
    @Transactional
    public User findOrCreateUser(String telegramId, String username, Long chatId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> createUser(telegramId, username, chatId));
    }


    /**
     * Обновить информацию о пользователе
     */
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Активировать пользователя
     */
    @Transactional
    public void activateUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.activate();
            userRepository.save(user);
            log.info("Пользователь {} активирован", user.getName());
        });
    }

    /**
     * Деактивировать пользователя
     */
    @Transactional
    public void deactivateUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.deactivate();
            userRepository.save(user);
            log.info("Пользователь {} деактивирован", user.getName());
        });
    }

    /**
     * Назначить пользователя администратором
     */
    @Transactional
    public void makeAdmin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.makeAdmin();
            userRepository.save(user);
            log.info("Пользователь {} назначен администратором", user.getName());
        });
    }

    /**
     * Получить всех активных пользователей
     */
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Получить всех администраторов
     */
    public List<User> getAllAdmins() {
        return userRepository.findByIsAdminTrue();
    }

    /**
     * Получить статистику пользователей
     */
    public UserStatistics getStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByIsActiveTrue().size();
        long admins = userRepository.findByIsAdminTrue().size();

        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .admins(admins)
                .inactiveUsers(totalUsers - activeUsers)
                .build();
    }

    /**
     * Проверить, является ли пользователь администратором
     */
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
    }

    /**
     * Проверить, является ли пользователь администратором по telegramId
     */
    public boolean isAdminByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(User::isAdmin)
                .orElse(false);
    }

    /**
     * Удалить пользователя
     */
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
        log.info("Пользователь с ID {} удален", userId);
    }

    /**
     * Создать пользователя с указанным именем (для регистрации из Telegram)
     */
    @Transactional
    public User createUser(String telegramId, String name, Long chatId) {
        // Проверяем, не существует ли уже пользователь с таким telegramId
        Optional<User> existingUser = userRepository.findByTelegramId(telegramId);
        if (existingUser.isPresent()) {
            log.info("Пользователь с telegramId {} уже существует", telegramId);
            return existingUser.get();
        }

        // Проверяем, не существует ли уже пользователь с таким chatId
        existingUser = userRepository.findByChatId(chatId);
        if (existingUser.isPresent()) {
            log.info("Пользователь с chatId {} уже существует", chatId);
            return existingUser.get();
        }

        // Создаем нового пользователя
        User user = User.builder()
                .telegramId(telegramId)
                .name(name)
                .chatId(chatId)
                .isActive(true)
                .isAdmin(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Создан новый пользователь: {} (telegramId: {}, chatId: {})",
                savedUser.getName(), telegramId, chatId);

        return savedUser;
    }

    /**
     * Класс для статистики пользователей
     */
    @Data
    @Builder
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long admins;

        public double getActivePercentage() {
            return totalUsers > 0 ? (activeUsers * 100.0) / totalUsers : 0;
        }

        public double getAdminPercentage() {
            return totalUsers > 0 ? (admins * 100.0) / totalUsers : 0;
        }
    }
}
