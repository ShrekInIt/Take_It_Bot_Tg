package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderHandler {

    private final TelegramBot bot;
    private final CartService cartService;
    private final MessageSender messageSender;

    /**
     * Обработка callback-запросов для заказов
     */
    public void handlerCartCallback(Long chatId, String callbackId, String data) {
        if (data.startsWith("order_create")) {
            log.info("Создание заказа...");
            handleCreateOrder(chatId, callbackId);
        }
    }

    /**
     * Создание заказа
     */
    private void handleCreateOrder(Long chatId, String callbackId) {
        try {
            if (cartService.isCartEmpty(chatId)) {
                messageSender.answerCallback(callbackId, "❌ Корзина пуста");
                messageSender.sendMessage(chatId, "❌ Ваша корзина пуста. Добавьте товары перед оформлением заказа.");
                return;
            }

            int totalAmount = cartService.getCartTotal(chatId);

            String message = String.format(
                    """
                    📝 *Оформление заказа*
                    
                    💰 *Сумма заказа:* %d₽
                    
                    Пожалуйста, выберите способ получения:
                    """,
                    totalAmount
            );

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton pickupButton = new InlineKeyboardButton("🚶 Самовывоз")
                    .callbackData("order_pickup");
            keyboard.addRow(pickupButton);

            InlineKeyboardButton deliveryButton = new InlineKeyboardButton("🚚 Доставка")
                    .callbackData("order_delivery");
            keyboard.addRow(deliveryButton);

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад в корзину")
                    .callbackData("cart_back");
            keyboard.addRow(backButton);

            SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(sendMessage);
            messageSender.answerCallback(callbackId, "Выберите способ получения");

        } catch (Exception e) {
            log.error("Ошибка создания заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при оформлении заказа");
            messageSender.sendMessage(chatId, "❌ Ошибка при оформлении заказа");
        }
    }
}
