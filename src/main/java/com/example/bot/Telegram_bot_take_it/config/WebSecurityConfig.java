package com.example.bot.Telegram_bot_take_it.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Конфигурация Spring Security для админ-панели и API.
 * <p>
 * Настраивает:
 *  - PasswordEncoder (BCrypt) для хеширования паролей
 *  - правила доступа к страницам и API по ролям
 *  - formLogin (страница /admin/login)
 *  - logout (/admin/logout)
 *  - обработку случая, когда пользователь не авторизован (редирект на /admin/login)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    /**
     * Регистрирует PasswordEncoder для безопасного хранения паролей.
     * <p>
     * BCrypt — стандартный алгоритм хеширования паролей.
     * Используется при создании/обновлении AdminUser (passwordHash).
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Основная настройка цепочки фильтров безопасности (SecurityFilterChain).
     * <p>
     * Логика доступа:
     *  - Разрешены без авторизации: статические ресурсы, /admin/login, /error и т.п.
     *  - /api/admin/auth/check и /api/admin/current-user доступны всем (для проверки авторизации)
     *  - /api/admin/products/** и /api/admin/categories/** доступны ролям ADMIN и SUPER_ADMIN
     *  - /api/admin/** (остальное) доступно только SUPER_ADMIN
     *  - страницы /admin/** доступны ADMIN и SUPER_ADMIN
     * <p>
     * Также:
     *  - отключен CSRF
     *  - настроен formLogin с обработкой /admin/login
     *  - настроен logout
     *  - если пользователь не авторизован — редирект на /admin/login
     *
     * @param http объект HttpSecurity для настройки правил
     * @return собранный SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/",
                                "/js/**",
                                "/css/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/admin/login",
                                "/admin/static/**"
                        ).permitAll()

                        .requestMatchers("/api/admin/auth/check", "/api/admin/current-user").authenticated()

                        .requestMatchers("/api/admin/products/**", "/api/admin/categories/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .requestMatchers("/api/admin/**")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers("/admin", "/admin/", "/admin/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .anyRequest().permitAll()
                )

                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/admin/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )

                .headers(headers -> headers
                        .httpStrictTransportSecurity(Customizer.withDefaults())
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/admin/login")
                        )
                );

        return http.build();
    }
}
