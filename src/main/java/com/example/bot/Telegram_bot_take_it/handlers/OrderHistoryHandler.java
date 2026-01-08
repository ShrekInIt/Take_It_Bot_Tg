package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.entity.OrderItemAddon;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderHistoryHandler {
    private final TelegramBot bot;
    private final OrderService orderService;
    private final MessageSender messageSender;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Обработка команды просмотра истории заказов
     */
    public void handleOrderHistory(Long chatId) {
        try {
            List<Order> orders = orderService.getUserOrders(chatId);

            if (orders.isEmpty()) {
                messageSender.sendMessage(chatId, "📭 У вас еще нет заказов.");
                return;
            }

            String message = createOrderHistoryMessage(orders);
            sendOrderHistoryMessage(chatId, message, createOrderHistoryKeyboard(orders));

        } catch (Exception e) {
            log.error("Ошибка при получении истории заказов: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при получении истории заказов.");
        }
    }

    /**
     * Создание сообщения с историей заказов
     */
    private String createOrderHistoryMessage(List<Order> orders) {
        StringBuilder message = new StringBuilder();
        message.append("📋 *История ваших заказов:*\n\n");

        int totalOrders = orders.size();
        int totalAmount = orders.stream().mapToInt(Order::getTotalAmount).sum();

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);

            message.append("📍 *Заказ #").append(i + 1).append("*\n");
            message.append("📦 *Номер:* `").append(order.getOrderNumber()).append("`\n");
            message.append("📅 *Дата:* ").append(order.getCreatedAt().format(DATE_FORMATTER)).append("\n");
            message.append("💰 *Сумма:* ").append(order.getTotalAmount()).append("₽\n");
            message.append("📊 *Статус:* ").append(getStatusEmoji(order.getStatus())).append(" ")
                    .append(order.getStatus().getDescription()).append("\n");
            message.append("🚚 *Тип:* ").append(order.getDeliveryType().getDescription()).append("\n");

            // Краткая информация о составе
            if (!order.getItems().isEmpty()) {
                message.append("📦 *Товаров:* ").append(order.getItems().size()).append("\n");
            }

            if (i < orders.size() - 1) {
                message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            }
        }

        message.append("\n📊 *Статистика:*\n");
        message.append("• Всего заказов: ").append(totalOrders).append("\n");
        message.append("• Общая сумма: ").append(totalAmount).append("₽\n");

        return message.toString();
    }

    /**
     * Создание клавиатуры для истории заказов
     */
    private InlineKeyboardMarkup createOrderHistoryKeyboard(List<Order> orders) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            InlineKeyboardButton detailsButton = new InlineKeyboardButton("📋 Подробнее #" + (i + 1))
                    .callbackData("order_details_" + order.getId());
            keyboard.addRow(detailsButton);
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("main_menu");
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Отправка сообщения с историей заказов
     */
    private void sendOrderHistoryMessage(Long chatId, String message, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Обработка callback для просмотра деталей заказа
     */
    public void handleOrderDetailsCallback(Long chatId, String callbackId, String data) {
        try {
            String orderIdStr = data.replace("order_details_", "");
            Long orderId = Long.parseLong(orderIdStr);

            Order order = orderService.getOrderByIdAndUser(orderId, chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден или не принадлежит вам"));

            String detailsMessage = createOrderDetailsMessage(order);
            sendOrderDetailsMessage(chatId, detailsMessage, order.getId());

            messageSender.answerCallback(callbackId, "✅ Детали заказа загружены");

        } catch (Exception e) {
            log.error("Ошибка при получении деталей заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при получении деталей заказа");
            messageSender.sendMessage(chatId, "❌ Не удалось загрузить детали заказа.");
        }
    }

    /**
     * Создание детального сообщения о заказе
     */
    private String createOrderDetailsMessage(Order order) {
        StringBuilder message = new StringBuilder();

        message.append("📋 *Детали заказа*\n\n");
        message.append("📦 *Номер заказа:* `").append(order.getOrderNumber()).append("`\n");
        message.append("📅 *Дата создания:* ").append(order.getCreatedAt().format(DATE_FORMATTER)).append("\n");
        message.append("💰 *Сумма заказа:* ").append(order.getTotalAmount()).append("₽\n");
        message.append("📊 *Статус:* ").append(getStatusEmoji(order.getStatus())).append(" ")
                .append(order.getStatus().getDescription()).append("\n");
        message.append("🚚 *Способ получения:* ").append(order.getDeliveryType().getDescription()).append("\n");

        if (order.getPhoneNumber() != null) {
            message.append("📱 *Телефон:* ").append(order.getPhoneNumber()).append("\n");
        }

        if (order.getAddress() != null) {
            message.append("🏠 *Адрес:* ").append(order.getAddress()).append("\n");
        }

        if (order.getComments() != null && !order.getComments().isEmpty()) {
            message.append("💬 *Комментарий:* ").append(order.getComments()).append("\n");
        }

        message.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        message.append("🛒 *Состав заказа:*\n\n");

        List<OrderItem> items = order.getItems();
        int itemNumber = 1;

        for (OrderItem item : items) {
            message.append(itemNumber).append(". *").append(item.getProductName()).append("*\n");
            message.append("   • Количество: ").append(item.getQuantity()).append("\n");
            message.append("   • Цена за единицу: ").append(item.getPriceAtOrder()).append("₽\n");
            message.append("   • Сумма: ").append(item.getPriceAtOrder() * item.getQuantity()).append("₽\n");

            List<OrderItemAddon> addons = item.getAddons();
            if (addons != null && !addons.isEmpty()) {
                message.append("   • Добавки:\n");
                for (OrderItemAddon addon : addons) {
                    message.append("      🍯 ").append(addon.getAddonProductName())
                            .append(" x").append(addon.getQuantity())
                            .append(" (+").append(addon.getPriceAtOrder() * addon.getQuantity()).append("₽)\n");
                }
            }

            message.append("\n");
            itemNumber++;
        }

        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("💰 *Итого к оплате:* ").append(order.getTotalAmount()).append("₽");

        return message.toString();
    }

    /**
     * Отправка детального сообщения о заказе
     */
    private void sendOrderDetailsMessage(Long chatId, String message, Long orderId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к истории")
                .callbackData("order_history");

        InlineKeyboardButton repeatButton = new InlineKeyboardButton("🔄 Повторить заказ")
                .callbackData("repeat_order_" + orderId);

        keyboard.addRow(backButton, repeatButton);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Получить эмодзи для статуса заказа
     */
    private String getStatusEmoji(Order.OrderStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case CONFIRMED -> "✅";
            case PREPARING -> "👨‍🍳";
            case READY -> "🚀";
            case DELIVERING -> "🚚";
            case COMPLETED -> "🎉";
            case CANCELLED -> "❌";
        };
    }
}
