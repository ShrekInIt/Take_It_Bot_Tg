package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.dto.response.OrderItemAddonResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.OrderItemResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.OrderResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
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

    private final OrderService orderService;
    private final MessageSender messageSender;
    private final CartService cartService;
    private final TelegramMessageSender telegramMessageSender;
    private final KeyboardService keyboardService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Обработка команды просмотра истории заказов
     */
    public void handleOrderHistory(Long chatId) {
        try {
            List<OrderResponseDto> orders = orderService.getUserOrdersDto(chatId);

            if (orders.isEmpty()) {
                messageSender.sendMessage(chatId, "📭 У вас еще нет заказов.");
                return;
            }

            String message = createOrderHistoryMessage();
            telegramMessageSender.sendMessageWithInlineKeyboard(chatId, message,
                    keyboardService.createOrderHistoryKeyboardDto(orders), true);

        } catch (Exception e) {
            log.error("Ошибка при получении истории заказов: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при получении истории заказов.");
        }
    }

    /**
     * Обработка команды просмотра истории заказов
     */
    public void handleOrderHistory(Long chatId, Integer messageId) {
        try {
            List<OrderResponseDto> orders = orderService.getUserOrdersDto(chatId);

            if (orders.isEmpty()) {
                messageSender.sendMessage(chatId, "📭 У вас еще нет заказов.");
                return;
            }

            String message = createOrderHistoryMessage();
            telegramMessageSender.sendEditMessage(chatId, messageId, message,
                    keyboardService.createOrderHistoryKeyboardDto(orders), true);

        } catch (Exception e) {
            log.error("Ошибка при получении истории заказов: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при получении истории заказов.");
        }
    }

    /**
     * Обработка повторения заказа
     */
    public void handleRepeatOrder(Long chatId, String callbackId, String data, Integer messageId) {
        try {
            String orderIdStr = data.replace("repeat_order_", "");
            Long orderId = Long.parseLong(orderIdStr);

            OrderResponseDto order = orderService.getOrderByIdAndUserDto(orderId, chatId)
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
                    order.getItems() != null ? order.getItems().size() : 0
            );

            telegramMessageSender.sendEditMessage(chatId, messageId,message,
                    keyboardService.createButtonBackBasket(),true);

        } catch (Exception e) {
            log.error("Ошибка при повторении заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка");
            messageSender.sendMessage(chatId, "❌ Не удалось повторить заказ: " + e.getMessage());
        }
    }

    /**
     * Создание сообщения с историей заказов
     */
    private String createOrderHistoryMessage() {
        return "📋 *История ваших заказов:*\n\n";
    }

    /**
     * Очистить историю заказов
     */
    public void clearHistoryHandler(Long chatId, Integer messageId) {
        try {
            List<OrderResponseDto> orders = orderService.getUserOrdersDto(chatId);

            if (orders.isEmpty()) {
                messageSender.sendMessage(chatId, "📭 У вас нет заказов для скрытия.");
                return;
            }

            List<OrderResponseDto> completedOrders = orders.stream()
                    .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatus()))
                    .toList();

            if (completedOrders.isEmpty()) {
                messageSender.sendMessage(chatId, "📭 У вас нет завершенных заказов для скрытия.");
                return;
            }

            int hiddenCount = orderService.hideCompletedOrders(chatId);

            List<OrderResponseDto> updatedOrders = orderService.getUserOrdersDto(chatId);

            String message;
            InlineKeyboardMarkup keyboard;

            if (updatedOrders.isEmpty()) {
                message = """
                        📭 *История заказов*
                        
                        ✅ Все завершенные заказы скрыты.
                        Активных заказов не найдено.
                        
                        Вы можете просмотреть скрытые заказы в настройках или создать новый заказ.""";

                keyboard = keyboardService.createButtonMainMenuBack();

            } else {
                message = createOrderHistoryMessage();
                keyboard = keyboardService.createOrderHistoryKeyboardDto(updatedOrders);
            }

            telegramMessageSender.sendEditMessage(chatId, messageId, message,
                    keyboard, true);

            messageSender.sendMessage(chatId, "✅ Скрыто завершенных заказов: " + hiddenCount);

            log.info("Скрыта история заказов для chatId {}: {} завершенных заказов",
                    chatId, hiddenCount);

        } catch (Exception e) {
            log.error("Ошибка при очистке истории заказов: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Произошла ошибка при очистке истории заказов.");
        }
    }

    /**
     * Обработка callback для просмотра деталей заказа
     */
    public void handleOrderDetailsCallback(Long chatId, String callbackId, String data, Integer messageId) {
        try {
            String orderIdStr = data.replace("order_details_", "");
            Long orderId = Long.parseLong(orderIdStr);

            OrderResponseDto order = orderService.getOrderByIdAndUserDto(orderId, chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден или не принадлежит вам"));

            String detailsMessage = createOrderDetailsMessage(order);

            telegramMessageSender.sendEditMessage(chatId, messageId, detailsMessage,
                    keyboardService.createKeyboardForOrders(orderId), true);

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
    private String createOrderDetailsMessage(OrderResponseDto order) {
        StringBuilder message = new StringBuilder();

        message.append("📋 *Детали заказа*\n\n");
        message.append("📦 *Номер заказа:* `").append(order.getOrderNumber()).append("`\n");
        message.append("📅 *Дата создания:* ")
                .append(order.getCreatedAt().format(DATE_FORMATTER))
                .append("\n");
        message.append("💰 *Сумма заказа:* ").append(order.getTotalAmount()).append("₽\n");

        String status = order.getStatus();
        message.append("📊 *Статус:* ")
                .append(keyboardService.getStatusEmoji(status))
                .append(" ")
                .append(getStatusDescription(status))
                .append("\n");

        message.append("🚚 *Способ получения:* ")
                .append(getDeliveryDescription(order.getDeliveryType()))
                .append("\n");

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

        Map<String, List<OrderItemResponseDto>> groupedItems = groupOrderItems(order.getItems());

        int itemNumber = 1;
        for (Map.Entry<String, List<OrderItemResponseDto>> entry : groupedItems.entrySet()) {
            List<OrderItemResponseDto> group = entry.getValue();
            OrderItemResponseDto firstItem = group.getFirst();

            int totalQuantity = group.stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
            long pricePerItem = firstItem.getPriceAtOrder() != null ? firstItem.getPriceAtOrder() : 0;
            long groupTotalPrice = pricePerItem * totalQuantity;

            long addonsPrice = 0;
            if (firstItem.getAddons() != null && !firstItem.getAddons().isEmpty()) {
                addonsPrice = firstItem.getAddons().stream()
                        .mapToLong(addon -> addon.getPriceAtOrder() * addon.getQuantity())
                        .sum() * totalQuantity;
                groupTotalPrice += addonsPrice;
            }

            message.append(itemNumber).append(". *").append(firstItem.getProductName()).append("*\n");

            if (firstItem.getAddons() != null && !firstItem.getAddons().isEmpty()) {
                message.append("   • Добавки: ");
                String addonsStr = firstItem.getAddons().stream()
                        .map(this::formatAddon)
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

    private String formatAddon(OrderItemAddonResponseDto addon) {
        String addonName = addon.getAddonProductName() != null ? addon.getAddonProductName() : "Добавка";
        return addonName + (addon.getQuantity() != null && addon.getQuantity() > 1 ? " x" + addon.getQuantity() : "");
    }

    private String getStatusDescription(String status) {
        if (status == null) {
            return "Неизвестно";
        }

        try {
            return Order.OrderStatus.valueOf(status.toUpperCase()).getDescription();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    private String getDeliveryDescription(String deliveryType) {
        if (deliveryType == null) {
            return "Не указано";
        }

        try {
            return Order.DeliveryType.valueOf(deliveryType.toUpperCase()).getDescription();
        } catch (IllegalArgumentException e) {
            return deliveryType;
        }
    }

    /**
     * Группировка OrderItem по продукту и добавкам
     */
    private Map<String, List<OrderItemResponseDto>> groupOrderItems(List<OrderItemResponseDto> orderItems) {
        Map<String, List<OrderItemResponseDto>> groupedMap = new LinkedHashMap<>();

        if (orderItems == null) {
            return groupedMap;
        }

        for (OrderItemResponseDto item : orderItems) {
            String key = generateGroupKey(item);

            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        return groupedMap;
    }

    /**
     * Генерация ключа для группировки OrderItem
     * Формат: productId_addonId1,addonId2,addonId3
     */
    private String generateGroupKey(OrderItemResponseDto orderItem) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(orderItem.getProductId());

        if (orderItem.getAddons() != null && !orderItem.getAddons().isEmpty()) {
            String addonsKey = orderItem.getAddons().stream()
                    .map(addon -> addon.getAddonProductId() != null
                            ? String.valueOf(addon.getAddonProductId())
                            : "")
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining(","));

            if (!addonsKey.isEmpty()) {
                keyBuilder.append("_").append(addonsKey);
            }
        }

        return keyBuilder.toString();
    }
}
