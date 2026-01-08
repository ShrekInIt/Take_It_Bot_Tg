package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.OrderItemAddon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemAddonRepository extends JpaRepository<OrderItemAddon, Long> {
}
