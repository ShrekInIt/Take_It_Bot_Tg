package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import com.example.bot.Telegram_bot_take_it.admin.service.AdminUserService;
import com.example.bot.Telegram_bot_take_it.dto.AdminUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAdminController {

    private final AdminUserService adminUserService;

    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody AdminUserRequest dto) {
        adminUserService.create(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins() {
        return ResponseEntity.ok(adminUserService.findAll());
    }

    @GetMapping("/admins/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        return adminUserService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admins/search")
    public ResponseEntity<?> searchAdmin(@RequestParam String username) {
        return adminUserService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.ok().build();
    }

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
