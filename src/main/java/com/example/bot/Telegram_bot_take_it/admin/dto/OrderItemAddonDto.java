package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemAddonDto {
    private Long id;

    private String name;

    private Integer quantity;

    private Integer price;

    public OrderItemAddonDto() {

    }

    public OrderItemAddonDto(
            Long id,
            String name,
            Integer quantity,
            Integer price
    ) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }
}
