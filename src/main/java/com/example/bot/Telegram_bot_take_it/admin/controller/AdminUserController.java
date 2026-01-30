package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUserDto());
    }

    @GetMapping("/users/recent")
    public ResponseEntity<List<AdminUserDto>> getRecentUsers() {
        return ResponseEntity.ok(userService.findRecentUsersDto(10));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        Optional<AdminUserDto> user = userService.findByIdUserDto(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<AdminUserDto>> search(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                userService.searchByName(name)
                        .stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> update(@PathVariable Long id, @RequestBody Map<String,Object> req) {
        User updated = userService.update(id, req);
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserDto> create(@RequestBody Map<String, Object> req) {
        User created = userService.create(req);
        return ResponseEntity.ok(toDto(created));
    }


    private AdminUserDto toDto(User c) {
        return AdminUserDto.builder()
                .id(c.getId())
                .name(c.getName())
                .isActive(c.getIsActive())
                .telegramId(c.getTelegramId())
                .isAdmin(c.getIsAdmin())
                .chatId(c.getChatId())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
