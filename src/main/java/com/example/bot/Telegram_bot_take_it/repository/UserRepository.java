package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Найти пользователя по telegramId
     */
    Optional<User> findByTelegramId(String telegramId);

    /**
     * Найти пользователя по chatId
     */
    Optional<User> findByChatId(Long chatId);

    /**
     * Найти всех активных пользователей
     */
    List<User> findByIsActiveTrue();

    /**
     * Найти всех администраторов
     */
    List<User> findByIsAdminTrue();
}
