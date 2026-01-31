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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;


    public List<AdminUser> findAll() {
        return adminUserRepository.findAll();
    }

    public Optional<AdminUser> findById(Long id) {
        return adminUserRepository.findById(id);
    }

    public Optional<AdminUser> findByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

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

    public void delete(Long id) {
        if (!adminUserRepository.existsById(id)) {
            throw new RuntimeException("Администратор не найден");
        }
        adminUserRepository.deleteById(id);
    }
}
