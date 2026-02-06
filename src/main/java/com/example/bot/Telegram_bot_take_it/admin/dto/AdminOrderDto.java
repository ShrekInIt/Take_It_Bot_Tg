package com.example.bot.Telegram_bot_take_it.admin.dto;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO для отображения заказа в админ-панели (например, в таблице заказов).
 * <p>
 * Содержит ключевые поля заказа + информацию о пользователе и доставке.
 * Lombok:
 *  - @Data генерирует геттеры/сеттеры и служебные методы
 *  - @Builder позволяет собирать объект через билдера
 *  - @AllArgsConstructor создаёт конструктор со всеми полями
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AdminOrderDto {

    /** ID заказа */
    private Long id;

    /** Номер заказа */
    private String orderNumber;

    /** Итоговая сумма заказа */
    private Integer totalAmount;

    /** Статус заказа (enum из сущности Order) */
    private Order.OrderStatus status;

    /** Тип доставки (enum из сущности Order) */
    private Order.DeliveryType deliveryType;

    /** Имя пользователя (для отображения в админке) */
    private String userName;

    /** Телефон, указанный в заказе */
    private String phoneNumber;

    /** Адрес доставки (если применимо) */
    private String address;

    /** Комментарий к заказу */
    private String comments;

    /** Дата/время создания заказа */
    private LocalDateTime createdAt;
}
