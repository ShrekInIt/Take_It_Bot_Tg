package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CartItemGroupDTO {
    private Product product;
    private List<CartItem> items;
    private List<CartItemAddon> addons;
    private int totalQuantity;
    private boolean isCoffee;

    public CartItemGroupDTO(Product product, List<CartItem> items, List<CartItemAddon> addons) {
        this.product = product;
        this.items = items;
        this.addons = addons;
        this.totalQuantity = calculateTotalQuantity();
        this.isCoffee = determineIfCoffee();
    }

    public CartItemGroupDTO(Product product, List<CartItem> items, List<CartItemAddon> addons, int totalQuantity, boolean isCoffee) {
        this.product = product;
        this.items = items;
        this.addons = addons;
        this.totalQuantity = totalQuantity;
        this.isCoffee = isCoffee;
    }

    private int calculateTotalQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().mapToInt(CartItem::getCountProduct).sum();
    }

    private boolean determineIfCoffee() {
        if (product == null) {
            return false;
        }
        return product.getCategoryId() == 3L;
    }
}
