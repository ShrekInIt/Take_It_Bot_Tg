package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminUserDto;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.jetbrains.annotations.NotNull;
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

    @Query("SELECT u FROM User u ORDER BY u.id")
    @NotNull List<User> findAll();

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();


    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<AdminUserDto> findAllOrderByCreatedAtDescUserDto();

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<AdminUserDto> findByIdUserDto(Long id);

    @Query("""
    select u from User u
    where lower(u.name) like lower(concat('%', :name, '%'))
""")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);

    Integer countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Получить последние N пользователей
    @Query(value = "SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTopNByOrderByCreatedAtDesc(@Param("limit") int limit);
}
