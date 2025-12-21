package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Cart;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Найти корзину по пользователю
     */
    Optional<Cart> findByUser(User user);
}
