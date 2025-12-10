package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
     * Найти пользователя по имени
     */
    Optional<User> findByName(String name);

    /**
     * Найти всех активных пользователей
     */
    List<User> findByIsActiveTrue();

    /**
     * Найти всех администраторов
     */
    List<User> findByIsAdminTrue();

    /**
     * Проверить существование пользователя по telegramId
     */
    boolean existsByTelegramId(String telegramId);

    /**
     * Проверить существование пользователя по chatId
     */
    boolean existsByChatId(Long chatId);

    /**
     * Найти пользователей по части имени
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchByName(@Param("query") String query);

    /**
     * Найти пользователей, созданных после указанной даты
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Найти пользователей по статусу активности
     */
    List<User> findByIsActive(Boolean isActive);
}
