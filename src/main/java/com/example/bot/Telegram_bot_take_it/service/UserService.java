package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.dto.request.CreateUserRequest;
import com.example.bot.Telegram_bot_take_it.dto.request.UpdateUserRequest;
import com.example.bot.Telegram_bot_take_it.dto.response.UserResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.mapper.UserMapper;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Найти пользователя по chatId
     */
    public Optional<User> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public Optional<UserResponseDto> getUserByChatIdDto(Long chatId) {
        return getUserByChatId(chatId)
                .map(userMapper::toResponseDto);
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

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    /**
     * Возвращает последних зарегистрированных пользователей для админки.
     */
    public List<AdminUserDto> findRecentUsersDto(int limit) {
        return userRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .map(this::toAdminUserDto)
                .toList();
    }


    public Optional<AdminUserDto> findByIdUserDto(Long id) {
        return userRepository.findById(id)
                .map(this::toAdminUserDto);
    }

    /**
     * Преобразует сущность User в AdminUserDto.
     */
    private AdminUserDto toAdminUserDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .telegramId(u.getTelegramId())
                .chatId(u.getChatId())
                .phoneNumber(u.getPhoneNumber())
                .createdAt(u.getCreatedAt())
                .isActive(u.getIsActive())
                .isAdmin(u.getIsAdmin())
                .build();
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
                        .phoneNumber(user.getPhoneNumber())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Создать пользователя с использованием DTO
     */
    @Transactional
    public User create(CreateUserRequest request) {
        log.info("Создание пользователя: {}", request.getName());
        User user = userMapper.toEntity(request);
        return userRepository.save(user);
    }

    /**
     * Создать пользователя (legacy метод для обратной совместимости)
     * @deprecated Используйте {@link #create(CreateUserRequest)} вместо этого
     */
    @Deprecated
    @Transactional
    public User create(Map<String, Object> req) {
        String name = getString(req, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }

        User user = new User();
        user.setName(name);
        user.setTelegramId(getString(req, "telegramId"));
        user.setChatId(getLong(req));
        user.setPhoneNumber(getString(req, "phoneNumber"));
        user.setIsAdmin(getBoolean(req, "isAdmin", false));
        user.setIsActive(getBoolean(req, "isActive", true));

        return userRepository.save(user);
    }

    /**
     * Обновить пользователя с использованием DTO
     */
    @Transactional
    public User update(Long id, UpdateUserRequest request) {
        log.info("Обновление пользователя с ID: {}", id);
        User user = getById(id);
        userMapper.updateEntity(user, request);
        return userRepository.save(user);
    }

    /**
     * Обновить пользователя (legacy метод для обратной совместимости)
     * @deprecated Используйте {@link #update(Long, UpdateUserRequest)} вместо этого
     */
    @Deprecated
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
            user.setChatId(getLong(req));
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

    private Boolean getBoolean(Map<String, Object> req, String key, Boolean def) {
        Object v = req.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    private Long getLong(Map<String, Object> req) {
        Object v = req.get("chatId");
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }
}
