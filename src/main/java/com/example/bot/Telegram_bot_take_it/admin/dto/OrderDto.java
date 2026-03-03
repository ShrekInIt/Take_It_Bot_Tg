package com.example.bot.Telegram_bot_take_it.admin.dto;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для подробного просмотра заказа в админке.
 * <p>
 * В отличие от AdminOrderDto (который чаще “для списка”), этот DTO содержит:
 *  - пользователя (UserDto)
 *  - позиции заказа (items)
 *  - дополнительные поля доставки/комментария
 */
@Getter
@Setter
public class OrderDto {

    private Long id;

    private LocalDateTime createdAt;

    private String status;

    private Long totalAmount;

    private String deliveryType;

    private String deliveryAddress;

    private String comment;

    private UserDto user;

    private List<OrderItemDto> items;

    /**
     * Конструктор для быстрого формирования DTO из ключевых данных заказа.
     * <p>
     * Здесь:
     *  - статус и тип доставки переводятся в строку через enum.toString()
     *  - создаётся вложенный UserDto и заполняется username и phoneNumber
     *
     * @param id           ID заказа
     * @param status       статус заказа (enum)
     * @param totalAmount  итоговая сумма
     * @param userName     имя/логин пользователя (кладётся в user.username)
     * @param userPhone    телефон пользователя (кладётся в user.phoneNumber)
     * @param deliveryType тип доставки (enum)
     * @param createdAt    дата создания
     * @param comment      комментарий к заказу
     */
    public OrderDto(Long id, Order.OrderStatus status, Long totalAmount, String userName, String userPhone, Order.DeliveryType deliveryType, LocalDateTime createdAt, String comment) {
        this.id = id;
        this.status = status.toString();
        this.totalAmount = totalAmount;
        this.user = new UserDto();
        this.user.setUsername(userName);
        this.user.setPhoneNumber(userPhone);
        this.deliveryType = deliveryType.toString();
        this.createdAt = createdAt;
        this.comment = comment;
    }

    /**
     * Пустой конструктор нужен для сериализации/десериализации
     * (например, Jackson при работе с JSON).
     */
    public OrderDto() {
    }
}
