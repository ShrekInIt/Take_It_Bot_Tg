package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }


    public List<User> findRecentUsers(int limit) {
        return userRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    public List<AdminUserDto> findRecentUsersDto(int limit) {
        return userRepository.findAllOrderByCreatedAtDescUserDto()
                .stream()
                .limit(limit)
                .toList();
    }


    public Optional<AdminUserDto> findByIdUserDto(Long id) {
        return userRepository.findByIdUserDto(id);
    }

    public List<AdminUserDto> findAllUserDto() {
        return userRepository.findAll().stream()
                .map(user -> AdminUserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .telegramId(user.getTelegramId())
                        .chatId(user.getChatId())
                        .isAdmin(user.getIsAdmin())
                        .isActive(user.getIsActive())
                        .phoneNumber(user.getPhoneNumber())  // Важно!
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public User create(Map<String, Object> req) {
        String name = getString(req, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }

        User user = new User();
        user.setName(name);
        user.setTelegramId(getString(req, "telegramId"));
        user.setChatId(getLong(req, "chatId"));
        user.setPhoneNumber(getString(req, "phoneNumber"));
        user.setIsAdmin(getBoolean(req, "isAdmin", false));
        user.setIsActive(getBoolean(req, "isActive", true));

        // createdAt НЕ трогаем — БД сама поставит CURRENT_TIMESTAMP
        return userRepository.save(user);
    }



    @Transactional
    public User update(Long id, Map<String, Object> req) {
        User user = getById(id);

        if (req.containsKey("name")) {
            String name = getString(req, "name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }
            user.setName(name);
        }

        if (req.containsKey("telegramId")) {
            user.setTelegramId(getString(req, "telegramId"));
        }

        if (req.containsKey("chatId")) {
            user.setChatId(getLong(req, "chatId"));
        }

        if (req.containsKey("phoneNumber")) {
            user.setPhoneNumber(getString(req, "phoneNumber"));
        }

        if (req.containsKey("isAdmin")) {
            user.setIsAdmin(getBoolean(req, "isAdmin", false));
        }

        if (req.containsKey("isActive")) {
            user.setIsActive(getBoolean(req, "isActive", true));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    // Количество новых пользователей за сегодня
    public Integer countNewUsersToday() {
        LocalDate today = LocalDate.now();
        return userRepository.countByCreatedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }



    @Transactional(readOnly = true)
    public List<User> searchByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + id));
    }

    private String getString(Map<String, Object> req, String key) {
        Object v = req.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> req, String key, Integer def) {
        Object v = req.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) return Integer.parseInt(s);
        return def;
    }

    private Boolean getBoolean(Map<String, Object> req, String key, Boolean def) {
        Object v = req.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    private Long getLong(Map<String, Object> req, String key) {
        Object v = req.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }
}
