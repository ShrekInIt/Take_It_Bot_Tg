package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.service.CartService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderHistoryHandler {
    private final TelegramBot bot;
    private final OrderService orderService;
    private final MessageSender messageSender;
    private final CartService cartService;

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
     * Обработка повторения заказа
     */
    public void handleRepeatOrder(Long chatId, String callbackId, String data) {
        try {
            String orderIdStr = data.replace("repeat_order_", "");
            Long orderId = Long.parseLong(orderIdStr);

            Order order = orderService.getOrderByIdAndUser(orderId, chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден или не принадлежит вам"));

            cartService.repeatOrder(chatId, order);

            messageSender.answerCallback(callbackId, "✅ Заказ добавлен в корзину");

            String message = String.format(
                    """
                            ✅ *Заказ успешно добавлен в корзину!*
                            
                            📦 *Номер заказа:* `%s`
                            📅 *Дата:* %s
                            🛒 *Товаров добавлено:* %d
                            
                            Перейдите в корзину для оформления заказа.""",
                    order.getOrderNumber(),
                    order.getCreatedAt().format(DATE_FORMATTER),
                    order.getItems().size()
            );

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            InlineKeyboardButton cartButton = new InlineKeyboardButton("🛒 Перейти в корзину")
                    .callbackData("cart_back");
            keyboard.addRow(cartButton);

            SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(sendMessage);

        } catch (Exception e) {
            log.error("Ошибка при повторении заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка");
            messageSender.sendMessage(chatId, "❌ Не удалось повторить заказ: " + e.getMessage());
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

            if (!order.getItems().isEmpty()) {
                Map<String, List<OrderItem>> groupedItems = groupOrderItems(order.getItems());
                message.append("📦 *Позиций:* ").append(groupedItems.size()).append("\n");
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
     * Создание детального сообщения о заказе (с группировкой одинаковых позиций)
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

        Map<String, List<OrderItem>> groupedItems = groupOrderItems(order.getItems());

        int itemNumber = 1;
        for (Map.Entry<String, List<OrderItem>> entry : groupedItems.entrySet()) {
            List<OrderItem> group = entry.getValue();
            OrderItem firstItem = group.getFirst();

            int totalQuantity = group.stream().mapToInt(OrderItem::getQuantity).sum();
            int pricePerItem = firstItem.getPriceAtOrder();
            int groupTotalPrice = pricePerItem * totalQuantity;

            int addonsPrice = 0;
            if (firstItem.getAddons() != null && !firstItem.getAddons().isEmpty()) {
                addonsPrice = firstItem.getAddons().stream()
                        .mapToInt(addon -> addon.getPriceAtOrder() * addon.getQuantity())
                        .sum() * totalQuantity;
                groupTotalPrice += addonsPrice;
            }

            message.append(itemNumber).append(". *").append(firstItem.getProductName()).append("*\n");

            if (firstItem.getAddons() != null && !firstItem.getAddons().isEmpty()) {
                message.append("   • Добавки: ");
                String addonsStr = firstItem.getAddons().stream()
                        .map(addon -> {
                            String addonName = addon.getAddonProductName() != null ?
                                    addon.getAddonProductName() : "Добавка";
                            return addonName + (addon.getQuantity() > 1 ? " x" + addon.getQuantity() : "");
                        })
                        .collect(Collectors.joining(", "));
                message.append(addonsStr).append("\n");
            }

            message.append("   • Количество: ").append(totalQuantity).append("\n");
            message.append("   • Цена за единицу: ").append(pricePerItem).append("₽\n");

            if (addonsPrice > 0) {
                message.append("   • Стоимость добавок: +").append(addonsPrice).append("₽\n");
            }

            message.append("   • Сумма: ").append(groupTotalPrice).append("₽\n\n");

            itemNumber++;
        }

        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("💰 *Итого к оплате:* ").append(order.getTotalAmount()).append("₽");

        return message.toString();
    }

    /**
     * Группировка OrderItem по продукту и добавкам
     */
    private Map<String, List<OrderItem>> groupOrderItems(List<OrderItem> orderItems) {
        Map<String, List<OrderItem>> groupedMap = new LinkedHashMap<>();

        for (OrderItem item : orderItems) {
            String key = generateGroupKey(item);

            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        return groupedMap;
    }

    /**
     * Генерация ключа для группировки OrderItem
     * Формат: productId_addonId1,addonId2,addonId3
     */
    private String generateGroupKey(OrderItem orderItem) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(orderItem.getProduct().getId());

        if (orderItem.getAddons() != null && !orderItem.getAddons().isEmpty()) {
            String addonsKey = orderItem.getAddons().stream()
                    .map(addon -> {
                        if (addon.getAddonProduct() != null) {
                            return String.valueOf(addon.getAddonProduct().getId());
                        }
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining(","));

            if (!addonsKey.isEmpty()) {
                keyBuilder.append("_").append(addonsKey);
            }
        }

        return keyBuilder.toString();
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
