package com.example.bot.Telegram_bot_take_it.admin.service;

import com.example.bot.Telegram_bot_take_it.admin.entity.AdminUser;
import com.example.bot.Telegram_bot_take_it.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Реализация UserDetailsService для Spring Security.
 * <p>
 * Spring Security вызывает этот сервис при логине, чтобы:
 *  - найти администратора по username
 *  - проверить, что он активен
 *  - собрать UserDetails (username/passwordHash/roles) для дальнейшей авторизации
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /** Репозиторий для поиска администратора по username */
    private final AdminUserRepository adminUserRepository;

    /**
     * Загружает пользователя (админа) по username для механизма аутентификации Spring Security.
     * <p>
     * Логика:
     *  - ищет AdminUser в базе по username
     *  - если не найден — кидает UsernameNotFoundException
     *  - если admin не активен — также кидает UsernameNotFoundException
     *  - возвращает UserDetails (Spring Security User) с:
     *      username
     *      passwordHash
     *      roles(admin.getRole())
     *
     * @param username логин администратора
     * @return UserDetails для Spring Security
     * @throws UsernameNotFoundException если админ не найден или неактивен
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {


        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));


        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new UsernameNotFoundException("Admin is not active");
        }

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles(admin.getRole())
                .build();
    }
}
