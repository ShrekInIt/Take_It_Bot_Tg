package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Cart;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Найти корзину по пользователю
     */
    Optional<Cart> findByUser(User user);

    /**
     * Найти корзину по ID пользователя
     */
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);

    /**
     * Найти корзину по Telegram ID пользователя
     */
    @Query("SELECT c FROM Cart c WHERE c.user.telegramId = :telegramId")
    Optional<Cart> findByUserTelegramId(@Param("telegramId") String telegramId);

    /**
     * Найти корзину по chatId пользователя
     */
    @Query("SELECT c FROM Cart c WHERE c.user.chatId = :chatId")
    Optional<Cart> findByUserChatId(@Param("chatId") Long chatId);

    /**
     * Проверить существование корзины у пользователя
     */
    boolean existsByUser(User user);

    /**
     * Удалить корзину по ID пользователя
     */
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
