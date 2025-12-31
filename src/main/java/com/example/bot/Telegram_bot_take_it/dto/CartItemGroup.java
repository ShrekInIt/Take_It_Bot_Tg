package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CartItemGroup {
    private final CartItem firstItem;
    private int totalQuantity;
    private int totalPrice;
    private final List<CartItem> items;

    public CartItemGroup(CartItem firstItem) {
        this.firstItem = firstItem;
        this.totalQuantity = firstItem.getCountProduct();
        this.totalPrice = firstItem.calculateItemTotal();
        this.items = new ArrayList<>();
        this.items.add(firstItem);
    }

    public void addItem(CartItem item) {
        this.totalQuantity += item.getCountProduct();
        this.totalPrice += item.calculateItemTotal();
        this.items.add(item);
    }
}
