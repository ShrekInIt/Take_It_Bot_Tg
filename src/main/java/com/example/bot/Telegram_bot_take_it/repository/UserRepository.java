package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT u FROM User u ORDER BY u.id")
    @NotNull List<User> findAll();

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

}
