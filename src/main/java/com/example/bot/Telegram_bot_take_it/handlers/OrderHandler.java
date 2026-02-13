package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.*;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final TelegramMessageSender telegramMessageSender;
    private final KeyboardService keyboardService;
    private final OrderSessionService orderSessionService;

    /**
     * Обработка callback-запросов для заказов
     */
    public void handlerCartCallback(Long chatId, String callbackId, String data, Integer messageId) {
        if (data.startsWith("order_create")) {
            log.info("Начало оформления заказа...");
            handleOrderStart(chatId, callbackId, messageId);
        } else if (data.startsWith("order_delivery_pickup")) {
            log.info("Выбран самовывоз");
            handleDeliveryTypeSelected(chatId, callbackId, messageId);
        } else if (data.startsWith("order_delivery_delivery")) {
            log.info("Выбрана доставка");
            messageSender.sendMessage(chatId, "К сожалению доставка пока недоступна");
        } else if (data.startsWith("order_confirm")) {
            log.info("Подтверждение заказа");
            handleOrderConfirm(chatId, callbackId, messageId);
        } else if (data.startsWith("order_cancel")) {
            log.info("Отмена заказа");
            handleOrderCancel(chatId, callbackId, messageId);
        } else if (data.equals("order_history")) {
            log.info("Обработка истории заказов...");
            orderHistoryHandler.handleOrderHistory(chatId, messageId);
        } else if (data.startsWith("order_details_")) {
            log.info("Обработка деталей заказа...");
            orderHistoryHandler.handleOrderDetailsCallback(chatId, callbackId, data, messageId);
        }else if (data.startsWith("order_clear_history")) {
            log.info("Обработка очистки истории...");
            orderHistoryHandler.clearHistoryHandler(chatId, messageId);
        } else if (data.startsWith("order_skip")) {
            handleTextMessage(chatId, "Пропустить", messageId);
        }
    }

    /**
     * Обработка контакта (номера телефона)
     */
    public void handleContact(Long chatId, String phoneNumber, Integer messageId) {
        try {
            log.info("Получен контакт от chatId {}: {}", chatId, phoneNumber);

            userService.updatePhoneNumber(chatId, phoneNumber);

            var optSession = orderSessionService.get(chatId);
            if (optSession.isPresent()) {
                var session = optSession.get();
                session.setPhoneNumber(phoneNumber);
                orderSessionService.save(session);

                ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
                SendMessage removeMessage = new SendMessage(chatId.toString(), "✅ Номер телефона получен")
                        .replyMarkup(removeKeyboard);
                bot.execute(removeMessage);

                askForDeliveryType(chatId, messageId);
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
    public void handleTextMessage(Long chatId, String text, Integer messageId) {
        try {
            log.info("Обработка текста в OrderHandler: chatId={}, text={}", chatId, text);

            var optSession = orderSessionService.get(chatId);

            if (optSession.isEmpty()) {
                if (isPhoneNumber(text)) {
                    processPhoneNumberInput(chatId, text, messageId);
                }
                return;
            }

            var session = optSession.get();

            String normalized = text;
            if (text.equalsIgnoreCase("пропустить") || text.equalsIgnoreCase("нет") ||
                    text.equalsIgnoreCase("skip") || text.equalsIgnoreCase("no")) {
                normalized = "Пропущено";
            }

            if (session.getDeliveryType() == null) {
                messageSender.sendMessage(chatId, "⚠️ Пожалуйста, сначала выберите способ получения заказа.");
                return;
            }

            if ("DELIVERY".equals(session.getDeliveryType())) {
                if (session.getAddress() == null) {
                    session.setAddress(normalized);
                    orderSessionService.save(session);
                    askForComments(chatId, messageId);
                    return;
                }
                if (session.getComments() == null) {
                    session.setComments(normalized);
                    orderSessionService.save(session);
                    showOrderConfirmation(chatId);
                    return;
                }
            }

            if ("PICKUP".equals(session.getDeliveryType())) {
                if (session.getComments() == null) {
                    session.setComments(normalized);
                    orderSessionService.save(session);
                    showOrderConfirmation(chatId);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке текстового сообщения для заказа: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при обработке сообщения. Попробуйте еще раз.");
        }
    }

    /**
     * Начало оформления заказа
     */
    private void handleOrderStart(Long chatId, String callbackId, Integer messageId) {
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

            var session = orderSessionService.createOrReset(chatId);

            if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
                askForPhoneNumber(chatId);
            } else {
                session.setPhoneNumber(user.getPhoneNumber());
                orderSessionService.save(session);
                askForDeliveryType(chatId, messageId);
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
    private void processPhoneNumberInput(Long chatId, String text, Integer messageId) {
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
                handleContact(chatId, phoneNumber, messageId);
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

        telegramMessageSender.sendMessageWithReplyKeyboard(chatId, message, keyboard, true);
    }

    /**
     * Запрос типа доставки
     */
    private void askForDeliveryType(Long chatId, Integer messageId) {
        String message = """
            🚚 *Выберите способ получения заказа*
            
            ⏰ Время работы: 10:00 - 21:00
            """;

        InlineKeyboardMarkup kb = keyboardService.createKeyboardForChoiceDelivery();

        telegramMessageSender.editOrSendMarkdown(chatId, messageId, message, kb);
    }

    /**
     * Обработка выбора типа доставки
     */
    private void handleDeliveryTypeSelected(Long chatId, String callbackId, Integer messageId) {
        var optSession = orderSessionService.get(chatId);
        if (optSession.isEmpty()) {
            messageSender.answerCallback(callbackId, "❌ Данные заказа не найдены");
            return;
        }

        var session = optSession.get();
        session.setDeliveryType("PICKUP");
        orderSessionService.save(session);

        askForComments(chatId, messageId);
        messageSender.answerCallback(callbackId, "✅ Способ получения: Самовывоз");
    }

    /**
     * Запрос комментариев к заказу
     */
    private void askForComments(Long chatId, Integer messageId) {
        String message = """
            💬 *Комментарий к заказу*
            
            Укажите дополнительную информацию:
            • Время для самовывоза (если выбрали самовывоз)
            • Особые пожелания
            • Прочее
            
            Если комментариев нет, нажмите кнопку "Пропустить"
            """;

        telegramMessageSender.sendEditMessage(chatId, messageId, message, keyboardService.createButtonSkip(), true);
    }

    /**
     * Показать подтверждение заказа
     */
    private void showOrderConfirmation(Long chatId) {
        var optSession = orderSessionService.get(chatId);
        if (optSession.isEmpty()) {
            messageSender.sendMessage(chatId, "❌ Данные заказа не найдены");
            return;
        }

        var session = optSession.get();

        int totalAmount = cartService.getCartTotal(chatId);

        String deliveryText = "PICKUP".equals(session.getDeliveryType())
                ? "🚶 Самовывоз"
                : "🚚 Доставка по адресу: " + session.getAddress();

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
                session.getPhoneNumber(),
                deliveryText,
                totalAmount,
                session.getComments() != null ? session.getComments() : "Нет"
        );

        telegramMessageSender.sendMessageWithInlineKeyboard(
                chatId,
                message,
                keyboardService.createKeyboardConfirmOrder(),
                true
        );
    }

    /**
     * Подтверждение и создание заказа
     */
    private void handleOrderConfirm(Long chatId, String callbackId, Integer messageId) {
        try {
            var optSession = orderSessionService.get(chatId);
            if (optSession.isEmpty()) {
                messageSender.answerCallback(callbackId, "❌ Данные заказа не найдены");
                return;
            }

            var session = optSession.get();

            OrderData orderData = OrderData.builder()
                    .chatId(chatId)
                    .cartItems(cartService.getCartItems(chatId))
                    .deliveryType(session.getDeliveryType())
                    .address(session.getAddress())
                    .comments(session.getComments())
                    .phoneNumber(session.getPhoneNumber())
                    .build();

            Order order = orderService.createOrderFromCart(chatId, orderData);

            cartService.clearCart(chatId);
            orderSessionService.clear(chatId);

            String orderMessage = createOrderConfirmationMessage(order);

            telegramMessageSender.sendEditMessage(
                    chatId,
                    messageId,
                    orderMessage,
                    keyboardService.createEmptyCartKeyboard(),
                    true
            );

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
                "🚚 *Способ получения:* Самовывоз\n" +
                ((order.getAddress() != null) ? "*Адрес:* " + order.getAddress() + "\n": "" ) +
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
    private void handleOrderCancel(Long chatId, String callbackId, Integer messageId) {
        orderSessionService.clear(chatId);

        String cartDescription = cartService.getCartDescription(chatId);

        telegramMessageSender.sendEditMessage(chatId, messageId, "❌ Заказ отменен\n\n" + cartDescription,
                keyboardService.createButtonBackBasket(), true);
        messageSender.answerCallback(callbackId, "❌ Заказ отменен");
    }
}
