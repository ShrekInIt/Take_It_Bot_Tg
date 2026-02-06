package com.example.bot.Telegram_bot_take_it.admin.service;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import com.example.bot.Telegram_bot_take_it.admin.repository.AdminUserRepository;
import com.example.bot.Telegram_bot_take_it.dto.AdminUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления администраторами (AdminUser).
 * <p>
 * Содержит бизнес-логику создания/обновления/удаления админов:
 *  - проверка уникальности username
 *  - хеширование пароля через PasswordEncoder
 *  - установка роли и активности
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    /** Репозиторий для CRUD-операций с сущностью AdminUser */
    private final AdminUserRepository adminUserRepository;

    /** Компонент для безопасного хеширования пароля перед сохранением в БД */
    private final PasswordEncoder passwordEncoder;

    /**
     * Возвращает список всех администраторов.
     *
     * @return список AdminUser
     */
    public List<AdminUser> findAll() {
        return adminUserRepository.findAll();
    }

    /**
     * Ищет администратора по ID.
     *
     * @param id идентификатор администратора
     * @return Optional с AdminUser (пустой, если не найден)
     */
    public Optional<AdminUser> findById(Long id) {
        return adminUserRepository.findById(id);
    }

    /**
     * Ищет администратора по username (логину).
     *
     * @param username логин администратора
     * @return Optional с AdminUser (пустой, если не найден)
     */
    public Optional<AdminUser> findByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

    /**
     * Создаёт нового администратора.
     * <p>
     * Логика:
     *  - проверяет, что username уникален
     *  - проверяет, что пароль не пустой
     *  - хеширует пароль и сохраняет passwordHash
     *  - устанавливает роль (если не задана — "ADMIN")
     *  - делает администратора активным (isActive = true)
     *
     * @param request входные данные (username, password, role и т.д.)
     * @return созданный AdminUser
     */
    public AdminUser create(AdminUserRequest request) {
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Администратор с таким логином уже существует");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(request.getUsername());

        adminUser.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        adminUser.setRole(
                request.getRole() != null ? request.getRole() : "ADMIN"
        );
        adminUser.setIsActive(true);

        return adminUserRepository.save(adminUser);
    }

    /**
     * Обновляет данные администратора по ID.
     * <p>
     * Обновляет только те поля, которые переданы в request:
     *  - username (если не null)
     *  - password (если не null и не пустой) — сохраняется в виде хеша
     *  - role (если не null)
     *  - isActive (если не null)
     *
     * @param id      ID администратора
     * @param request новые значения полей
     * @return обновлённый AdminUser
     */
    public AdminUser update(Long id, AdminUserRequest request) {
        AdminUser adminUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Администратор не найден"));

        if (request.getUsername() != null) {
            adminUser.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            adminUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            adminUser.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            adminUser.setIsActive(request.getIsActive());
        }

        return adminUserRepository.save(adminUser);
    }

    /**
     * Удаляет администратора по ID.
     * <p>
     * Перед удалением проверяет существование записи.
     *
     * @param id ID администратора
     */
    public void delete(Long id) {
        if (!adminUserRepository.existsById(id)) {
            throw new RuntimeException("Администратор не найден");
        }
        adminUserRepository.deleteById(id);
    }
}
