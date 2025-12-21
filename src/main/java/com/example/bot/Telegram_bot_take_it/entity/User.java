package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "telegram_id", unique = true)
    private String telegramId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Дополнительные методы для удобства

    /**
     * Проверить, является ли пользователь администратором
     */
    public boolean isAdmin() {
        return Boolean.TRUE.equals(isAdmin);
    }

    /**
     * Проверить, активен ли пользователь
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Активировать пользователя
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Деактивировать пользователя
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Назначить администратором
     */
    public void makeAdmin() {
        this.isAdmin = true;
    }
}
