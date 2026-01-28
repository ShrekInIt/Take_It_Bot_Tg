package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderItemDto {
    private Long id;

    private Integer quantity;

    private Integer price;

    private ProductDto product;

    private List<OrderItemAddonDto> addons;

    public OrderItemDto(Long id, ProductDto product, Integer quantity, Integer price) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public OrderItemDto(){}
}
