package com.example.bot.Telegram_bot_take_it.utils.interfaces;

/**
 * Порт отправки заказа во внешнюю кондитерскую систему.
 */
public interface ConfectioneryClient {
    void sendOrderToConfectionery(Object orderRequest);
}
