package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO позиции заказа (order item).
 * <p>
 * Хранит:
 *  - товар (ProductDto)
 *  - количество и цену
 *  - список добавок (addons), если они есть
 */
@Getter
@Setter
public class OrderItemDto {
    private Long id;

    private Integer quantity;

    private Long price;

    private ProductDto product;

    private List<OrderItemAddonDto> addons;

    /**
     * Конструктор для создания позиции заказа из основных данных.
     *
     * @param id       ID позиции
     * @param product  товар (упрощённый ProductDto)
     * @param quantity количество
     * @param price    цена позиции
     */
    public OrderItemDto(Long id, ProductDto product, Integer quantity, Long price) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Пустой конструктор — нужен для сериализации/десериализации (Jackson).
     */
    public OrderItemDto(){}
}
