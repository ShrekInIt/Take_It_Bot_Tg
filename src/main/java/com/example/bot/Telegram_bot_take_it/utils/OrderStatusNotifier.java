package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.utils.interfaces.OrderUserNotifier;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderStatusNotifier implements OrderUserNotifier {

    private final TelegramBot bot;

    /**
     * Отправляет пользователю Telegram-уведомление
     * об изменении статуса заказа.
     * Сообщение формируется в формате Markdown и содержит:
     * - номер заказа
     * - текущий статус
     * - дополнительную информацию в зависимости от статуса
     *
     * @param order заказ, по которому отправляется уведомление
     */
    @Override
    public void sendStatusUpdateNotification(Order order) {
        try {
            Long chatId = order.getUser().getChatId();
            String message = createStatusUpdateMessage(order);

            SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                    .parseMode(ParseMode.Markdown);

            bot.execute(sendMessage);
            log.info("Уведомление о статусе заказа {} отправлено пользователю {}",
                    order.getOrderNumber(), chatId);

        } catch (Exception e) {
            log.error("Ошибка отправки уведомления о статусе заказа: {}", e.getMessage(), e);
        }
    }

    /**
     * Формирует текст уведомления об изменении статуса заказа.
     * Сообщение включает:
     * - заголовок уведомления
     * - номер заказа
     * - текущий статус с эмодзи
     * - дополнительную информацию для пользователя
     * @param order заказ, для которого формируется сообщение
     * @return готовый текст сообщения в формате Markdown
     */
    private String createStatusUpdateMessage(Order order) {
        String statusEmoji = getStatusEmoji(order.getStatus());
        String statusText = order.getStatus().getDescription();

        return String.format("""
            📦 *Обновление статуса заказа*
            
            📋 *Номер заказа:* `%s`
            🔄 *Статус:* %s %s
            
            %s
            """,
                order.getOrderNumber(),
                statusEmoji,
                statusText,
                getStatusAdditionalInfo(order.getStatus())
        );
    }

    /**
     * Возвращает эмодзи, соответствующий текущему статусу заказа.
     * Используется для наглядного отображения этапа обработки заказа
     * в уведомлениях пользователю.
     * @param status текущий статус заказа
     * @return строка с эмодзи для указанного статуса
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

    /**
     * Возвращает дополнительный текст для уведомления пользователя
     * в зависимости от статуса заказа.
     * Текст может содержать:
     * - пояснение текущего этапа
     * - ориентировочное время ожидания
     * - инструкции для пользователя
     * @param status текущий статус заказа
     * @return дополнительный текст уведомления
     */
    private String getStatusAdditionalInfo(Order.OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "*Ваш заказ подтверждён!*\n\nНаши кондитеры уже начали готовить ваш заказ. Обычно приготовление занимает 20-30 минут.";
            case PREPARING -> "*Ваш заказ готовится!*\n\nКондитеры активно работают над вашим заказом. Осталось совсем немного!";
            case READY -> "*Заказ готов к выдаче!*\n\nПриходите забирать ваш вкусный заказ по адресу: Проспект врача сурова 36";
            case DELIVERING -> "*Курьер в пути!*\n\nНаш курьер уже везёт ваш заказ. Примерное время доставки: 15-20 минут.";
            case COMPLETED -> "*Заказ завершён!*\n\nСпасибо за заказ! Надеемся, вам понравилось! 😊";
            case CANCELLED -> "*Заказ отменён.*\n\nЕсли у вас возникли вопросы, свяжитесь с нами.";
            default -> "";
        };
    }
}
