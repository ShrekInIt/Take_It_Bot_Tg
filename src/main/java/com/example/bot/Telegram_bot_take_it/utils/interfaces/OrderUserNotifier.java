package com.example.bot.Telegram_bot_take_it.utils.interfaces;

import com.example.bot.Telegram_bot_take_it.entity.Order;

/**
 * Порт уведомления пользователя о статусе заказа.
 */
public interface OrderUserNotifier {
    void sendStatusUpdateNotification(Order order);
}
