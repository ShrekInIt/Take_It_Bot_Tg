package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

/**
 * Сессия оформления заказа (временное состояние диалога).
 * Хранится в БД, чтобы не теряться при перезапуске приложения.
 */
@Entity
@Table(name = "order_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSession {
    @Id
    private Long chatId;

    @Column(length = 32)
    private String deliveryType;

    @Column(length = 512)
    private String address;

    @Column(length = 512)
    private String comments;

    @Column(length = 32)
    private String phoneNumber;

    private Instant updatedAt;
}
