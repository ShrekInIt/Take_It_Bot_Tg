package com.example.bot.Telegram_bot_take_it.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDtoBot {
    private String productName;
    private Integer quantity;
    private Integer pricePerItem;
    private Integer totalPrice;
    private List<String> addons;

    public OrderItemDtoBot(String productName, int quantity, int pricePerItem, int totalPrice, List<String> addons) {
        this.productName = productName;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.totalPrice = totalPrice;
        this.addons = addons;
    }
}
