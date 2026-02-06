package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import com.example.bot.Telegram_bot_take_it.admin.service.AdminUserService;
import com.example.bot.Telegram_bot_take_it.dto.AdminUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления администраторами системы.
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Позволяет:
 *  - создавать администраторов
 *  - получать список администраторов
 *  - искать администратора по id и username
 *  - обновлять данные администратора
 *  - удалять администратора
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminAdminController {

    /**
     * Сервис для работы с администраторами (бизнес-логика)
     */
    private final AdminUserService adminUserService;

    /**
     * Создаёт нового администратора на основе переданных данных
     *
     * @param dto данные администратора (логин, пароль и т.д.)
     * @return HTTP 200 OK при успешном создании
     */
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody AdminUserRequest dto) {
        adminUserService.create(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Возвращает список всех администраторов в системе
     *
     * @return список администраторов
     */
    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    /**
     * Получает администратора по его идентификатору
     *
     * @param id ID администратора
     * @return администратор или 404, если не найден
     */
    @GetMapping("/admins/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        return adminUserService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ищет администратора по имени пользователя (username)
     *
     * @param username логин администратора
     * @return администратор или 404, если не найден
     */
    @GetMapping("/admins/search")
    public ResponseEntity<?> searchAdmin(@RequestParam String username) {
        return adminUserService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет администратора по ID
     *
     * @param id ID администратора
     * @return HTTP 200 OK после удаления
     */
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Обновляет данные администратора
     * <p>
     * Может изменять логин, пароль и другие поля.
     * Если пароль не передан — он не изменяется.
     *
     * @param id  ID администратора
     * @param dto новые данные администратора
     * @return обновлённый объект администратора или ошибка
     */
    @PutMapping("/admins/{id}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable Long id,
            @RequestBody AdminUserRequest dto
    ) {
        log.info("Обновление администратора ID: {}, данные: {}", id, dto);
        log.info("Пароль передан: {}", dto.getPassword() != null ? "Да" : "Нет");

        try {
            AdminUser updated = adminUserService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Ошибка обновления администратора", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
