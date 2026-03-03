package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO добавки (addon) к позиции заказа.
 * <p>
 * Пример: сироп, дополнительный ингредиент и т.п.
 * Содержит количество и цену добавки.
 */
@Getter
@Setter
public class OrderItemAddonDto {
    private Long id;

    private String name;

    private Integer quantity;

    private Long price;

    /**
     * Пустой конструктор — нужен для сериализации/десериализации (Jackson).
     */
    public OrderItemAddonDto() {

    }

    /**
     * Конструктор для удобного создания DTO добавки с полным набором полей.
     *
     * @param id       ID добавки
     * @param name     название добавки
     * @param quantity количество добавок
     * @param price    цена добавки
     */
    public OrderItemAddonDto(
            Long id,
            String name,
            Integer quantity,
            Long price
    ) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }
}
