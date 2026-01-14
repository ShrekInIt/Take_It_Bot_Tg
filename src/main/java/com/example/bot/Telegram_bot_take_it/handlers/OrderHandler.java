package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderHandler {
    private final TelegramBot bot;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;
    private final MessageSender messageSender;
    private final OrderHistoryHandler orderHistoryHandler;

    // Мапа для хранения временных данных заказа
    private final Map<Long, OrderData> orderDataMap = new ConcurrentHashMap<>();

    /**
     * Обработка callback-запросов для заказов
     */
    public void handlerCartCallback(Long chatId, String callbackId, String data, Integer messageId) {
        if (data.startsWith("order_create")) {
            log.info("Начало оформления заказа...");
            handleOrderStart(chatId, callbackId);
        } else if (data.startsWith("order_delivery_pickup")) {
            log.info("Выбран самовывоз");
            handleDeliveryTypeSelected(chatId, callbackId);
        } else if (data.startsWith("order_delivery_delivery")) {
            log.info("Выбрана доставка");
            messageSender.sendMessage(chatId, "К сожалению доставка пока недоступна");
        } else if (data.startsWith("order_confirm")) {
            log.info("Подтверждение заказа");
            handleOrderConfirm(chatId, callbackId);
        } else if (data.startsWith("order_cancel")) {
            log.info("Отмена заказа");
            handleOrderCancel(chatId, callbackId);
        } else if (data.equals("order_history")) {
            log.info("Обработка истории заказов...");
            orderHistoryHandler.handleOrderHistory(chatId);
        } else if (data.startsWith("order_details_")) {
            log.info("Обработка деталей заказа...");
            orderHistoryHandler.handleOrderDetailsCallback(chatId, callbackId, data);
        }else if (data.startsWith("order_clear_history")) {
            log.info("Обработка очистки истории...");
            orderHistoryHandler.clearHistoryHandler(chatId, messageId);
        } else if (data.startsWith("order_skip")) {
            handleTextMessage(chatId, "Пропустить");
        }
    }

    /**
     * Обработка контакта (номера телефона)
     */
    public void handleContact(Long chatId, String phoneNumber) {
        try {
            log.info("Получен контакт от chatId {}: {}", chatId, phoneNumber);

            userService.updatePhoneNumber(chatId, phoneNumber);

            OrderData orderData = orderDataMap.get(chatId);
            if (orderData != null) {
                orderData.setPhoneNumber(phoneNumber);
                orderDataMap.put(chatId, orderData);

                ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
                SendMessage removeMessage = new SendMessage(chatId.toString(), "✅ Номер телефона получен")
                        .replyMarkup(removeKeyboard);
                bot.execute(removeMessage);

                askForDeliveryType(chatId);
            } else {
                messageSender.sendMessage(chatId, "✅ Номер телефона сохранен. Теперь вы можете оформить заказ.");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке контакта: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при обработке номера телефона");
        }
    }

    /**
     * Обработка текстовых сообщений для оформления заказа
     */
    public void handleTextMessage(Long chatId, String text) {
        try {
            log.info("Обработка текста в OrderHandler: chatId={}, text={}", chatId, text);

            OrderData orderData = orderDataMap.get(chatId);

            if (orderData == null) {
                if (isPhoneNumber(text)) {
                    processPhoneNumberInput(chatId, text);
                }
                return;
            }

            if (text.equalsIgnoreCase("пропустить") || text.equalsIgnoreCase("нет") ||
                    text.equalsIgnoreCase("skip") || text.equalsIgnoreCase("no")) {
                text = "Пропущено";
            }

            if (orderData.getDeliveryType() != null &&
                    orderData.getDeliveryType().equals("DELIVERY") &&
                    orderData.getAddress() == null) {

                orderData.setAddress(text);
                orderDataMap.put(chatId, orderData);
                askForComments(chatId);

            }
            else if ((orderData.getDeliveryType() != null && orderData.getDeliveryType().equals("PICKUP")) ||
                    (orderData.getAddress() != null && orderData.getComments() == null)) {

                orderData.setComments(text);
                orderDataMap.put(chatId, orderData);
                showOrderConfirmation(chatId);
            }
            else if (orderData.getDeliveryType() == null) {
                messageSender.sendMessage(chatId, "⚠️ Пожалуйста, сначала выберите способ получения заказа.");
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке текстового сообщения для заказа: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при обработке сообщения. Попробуйте еще раз.");
        }
    }

    /**
     * Начало оформления заказа
     */
    private void handleOrderStart(Long chatId, String callbackId) {
        try {
            if (cartService.isCartEmpty(chatId)) {
                messageSender.answerCallback(callbackId, "❌ Корзина пуста");
                messageSender.sendMessage(chatId, "❌ Ваша корзина пуста. Добавьте товары перед оформлением заказа.");
                return;
            }

            List<String> unavailableItems = checkProductAvailability(chatId);
            if (!unavailableItems.isEmpty()) {
                String unavailableMessage = createUnavailableMessage(unavailableItems);
                messageSender.answerCallback(callbackId, "❌ Некоторые товары недоступны");
                messageSender.sendMessage(chatId, unavailableMessage);
                return;
            }

            Optional<User> userOpt = userService.getUserByChatId(chatId);
            if (userOpt.isEmpty()) {
                messageSender.answerCallback(callbackId, "❌ Пользователь не найден");
                return;
            }

            User user = userOpt.get();

            OrderData orderData = OrderData.builder()
                    .chatId(chatId)
                    .cartItems(cartService.getCartItems(chatId))
                    .build();
            orderDataMap.put(chatId, orderData);

            if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
                askForPhoneNumber(chatId);
            } else {
                orderData.setPhoneNumber(user.getPhoneNumber());
                orderDataMap.put(chatId, orderData);
                askForDeliveryType(chatId);
            }

            messageSender.answerCallback(callbackId, "Начинаем оформление заказа");

        } catch (Exception e) {
            log.error("Ошибка при начале оформления заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при оформлении заказа");
            messageSender.sendMessage(chatId, "❌ Произошла ошибка при оформлении заказа. Попробуйте позже.");
        }
    }

    /**
     * Проверка, похож ли текст на номер телефона
     */
    private boolean isPhoneNumber(String text) {
        String cleaned = text.replaceAll("[^0-9+]", "");
        return cleaned.length() >= 10 && cleaned.matches(".*[0-9]{10,}.*");
    }

    /**
     * Обработка введенного номера телефона
     */
    private void processPhoneNumberInput(Long chatId, String text) {
        try {
            log.info("Обработка ввода номера телефона: chatId={}, text={}", chatId, text);

            String cleanedPhone = text.replaceAll("[^0-9+]", "");

            String phoneNumber = null;

            if (cleanedPhone.startsWith("+7") && cleanedPhone.length() == 12) {
                phoneNumber = cleanedPhone;
            } else if (cleanedPhone.startsWith("8") && cleanedPhone.length() == 11) {
                phoneNumber = "+7" + cleanedPhone.substring(1);
            } else if (cleanedPhone.startsWith("7") && cleanedPhone.length() == 11) {
                phoneNumber = "+" + cleanedPhone;
            }

            if (phoneNumber != null) {
                log.info("Распознан номер телефона: {}", phoneNumber);
                handleContact(chatId, phoneNumber);
            } else {
                log.warn("Не удалось распознать номер телефона: {}", text);
                messageSender.sendMessage(chatId, "❌ Неверный формат номера телефона. Пожалуйста, используйте формат: +79991234567 или 89991234567");
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке ввода номера телефона: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при обработке номера телефона");
        }
    }

    /**
     * Проверка доступности товаров
     */
    private List<String> checkProductAvailability(Long chatId) {
        List<String> unavailableItems = new ArrayList<>();
        List<CartItem> cartItems = cartService.getCartItems(chatId);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null) {
                unavailableItems.add("Неизвестный товар");
                continue;
            }

            if (!product.getAvailable() || product.getCount() < cartItem.getCountProduct()) {
                unavailableItems.add(String.format("%s (осталось: %d, нужно: %d)",
                        product.getName(),
                        product.getCount(),
                        cartItem.getCountProduct()));
            }
        }

        return unavailableItems;
    }

    /**
     * Создание сообщения о недоступных товарах
     */
    private String createUnavailableMessage(List<String> unavailableItems) {
        StringBuilder message = new StringBuilder();
        message.append("❌ *Некоторые товары в вашей корзине недоступны:*\n\n");

        for (String item : unavailableItems) {
            message.append("• ").append(item).append("\n");
        }

        message.append("\nПожалуйста, удалите эти товары из корзины или измените их количество.");
        return message.toString();
    }

    /**
     * Запрос номера телефона
     */
    private void askForPhoneNumber(Long chatId) {
        String message = """
            📱 *Для оформления заказа нужен ваш номер телефона*
            
            Мы позвоним вам, чтобы подтвердить заказ и уточнить детали.
            
            Пожалуйста, нажмите кнопку ниже, чтобы поделиться номером телефона:
            """;

        KeyboardButton phoneButton = new KeyboardButton("📱 Отправить номер телефона")
                .requestContact(true);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(phoneButton)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .selective(true);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Запрос типа доставки
     */
    private void askForDeliveryType(Long chatId) {
        String message = """
            🚚 *Выберите способ получения заказа*
            
            ⏰ Время работы: 10:00 - 21:00
            """;

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton pickupButton = new InlineKeyboardButton("🚶 Самовывоз")
                .callbackData("order_delivery_pickup");

        InlineKeyboardButton deliveryButton = new InlineKeyboardButton("🚚 Доставка")
                .callbackData("order_delivery_delivery");

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отменить заказ")
                .callbackData("order_cancel");

        keyboard.addRow(pickupButton, deliveryButton);
        keyboard.addRow(cancelButton);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Обработка выбора типа доставки
     */
    private void handleDeliveryTypeSelected(Long chatId, String callbackId) {
        OrderData orderData = orderDataMap.get(chatId);
        if (orderData == null) {
            messageSender.answerCallback(callbackId, "❌ Данные заказа не найдены");
            return;
        }

        orderData.setDeliveryType("PICKUP");
        orderDataMap.put(chatId, orderData);

        askForComments(chatId);

        messageSender.answerCallback(callbackId, "✅ Способ получения: Самовывоз");
    }

    /**
     * Запрос комментариев к заказу
     */
    private void askForComments(Long chatId) {
        String message = """
            💬 *Комментарий к заказу*
            
            Укажите дополнительную информацию:
            • Время для самовывоза (если выбрали самовывоз)
            • Особые пожелания
            • Прочее
            
            Если комментариев нет, нажмите кнопку "Пропустить"
            """;

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton skipButton = new InlineKeyboardButton("Пропустить")
                .callbackData("order_skip");

        keyboard.addRow(skipButton);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Показать подтверждение заказа
     */
    private void showOrderConfirmation(Long chatId) {
        OrderData orderData = orderDataMap.get(chatId);
        if (orderData == null) {
            messageSender.sendMessage(chatId, "❌ Данные заказа не найдены");
            return;
        }

        int totalAmount = cartService.getCartTotal(chatId);
        String deliveryText = orderData.getDeliveryType().equals("PICKUP")
                ? "🚶 Самовывоз"
                : "🚚 Доставка по адресу: " + orderData.getAddress();

        String message = String.format("""
            ✅ *Подтверждение заказа*
            
            📱 *Телефон:* %s
            %s
            💰 *Сумма заказа:* %d₽
            
            *Комментарий:* %s
            
            *ВАЖНО:*
            📞 Мы позвоним вам в течение *20 минут* для подтверждения заказа.
            ⏰ Если мы не дозвонимся, пожалуйста, перезвоните нам по номеру +79930990947.
            
            Подтвердить заказ?
            """,
                orderData.getPhoneNumber(),
                deliveryText,
                totalAmount,
                orderData.getComments() != null ? orderData.getComments() : "Нет"
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Подтвердить заказ")
                .callbackData("order_confirm");

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отменить заказ")
                .callbackData("order_cancel");

        keyboard.addRow(confirmButton);
        keyboard.addRow(cancelButton);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Подтверждение и создание заказа
     */
    private void handleOrderConfirm(Long chatId, String callbackId) {
        try {
            OrderData orderData = orderDataMap.get(chatId);
            if (orderData == null) {
                messageSender.answerCallback(callbackId, "❌ Данные заказа не найдены");
                return;
            }

            Order order = orderService.createOrderFromCart(chatId, orderData);

            cartService.clearCart(chatId);

            orderDataMap.remove(chatId);

            String orderMessage = createOrderConfirmationMessage(order);

            messageSender.sendMessage(chatId, orderMessage);
            messageSender.answerCallback(callbackId, "✅ Заказ оформлен");

        } catch (Exception e) {
            log.error("Ошибка при подтверждении заказа: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при оформлении заказа");
            messageSender.sendMessage(chatId, "❌ Произошла ошибка при оформлении заказа. Попробуйте позже.");
        }
    }

    /**
     * Создание сообщения с подтверждением заказа
     */
    private String createOrderConfirmationMessage(Order order) {

        return "🎉 *Ваш заказ оформлен!*\n\n" +
                "📋 *Номер заказа:* " + order.getOrderNumber() + "\n" +
                "💰 *Сумма:* " + order.getTotalAmount() + "₽\n" +
                "📱 *Телефон:* " + order.getPhoneNumber() + "\n" +
                "🚚 *Способ получения:* Доставка\n" +
                "📍 *Адрес:* " + order.getAddress() + "\n" +
                "\n" +
                "⏰ *Примерное время:* 30-45 минут\n\n" +
                "*ВАЖНАЯ ИНФОРМАЦИЯ:*\n" +
                "📞 Мы позвоним вам в течение *20 минут* для подтверждения заказа.\n" +
                "⏰ Если мы не дозвонимся, пожалуйста, перезвоните нам по номеру:\n" +
                "📱 +79930990947\n\n" +
                "Спасибо за заказ! 😊";
    }

    /**
     * Отмена заказа
     */
    private void handleOrderCancel(Long chatId, String callbackId) {
        orderDataMap.remove(chatId);

        String cartDescription = cartService.getCartDescription(chatId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton backButton = new InlineKeyboardButton("🛒 Вернуться в корзину")
                .callbackData("cart_back");

        keyboard.addRow(backButton);

        SendMessage message = new SendMessage(chatId.toString(), "❌ Заказ отменен\n\n" + cartDescription)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(message);
        messageSender.answerCallback(callbackId, "❌ Заказ отменен");
    }
}
