package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для получения информации о текущей авторизации пользователя.
 * <p>
 * Используется админской частью (frontend/панель) чтобы:
 *  - проверить, авторизован ли пользователь
 *  - получить username и роль текущего пользователя
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    /**
     * Проверяет, есть ли текущая авторизация в SecurityContext.
     * <p>
     * Если пользователь залогинен (не anonymousUser), возвращает:
     *  - authenticated = true
     *  - user: { username, role }
     * <p>
     * Если пользователь не авторизован — возвращает:
     *  - authenticated = false
     *
     * @return JSON с признаком авторизации и (если есть) данными пользователя
     */
    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", authentication.getName());

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse(null);

            userInfo.put("role", role);

            response.put("authenticated", true);
            response.put("user", userInfo);

        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает информацию о текущем пользователе из SecurityContext.
     * <p>
     * Если пользователь авторизован (не anonymousUser), возвращает:
     *  - { username, role }
     * <p>
     * Если пользователь не авторизован — возвращает пустой объект {}
     *
     * @return JSON с данными текущего пользователя или пустой объект, если он не залогинен
     */
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", authentication.getName());

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse(null);

            userInfo.put("role", role);

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.ok(Collections.emptyMap());
    }
}
