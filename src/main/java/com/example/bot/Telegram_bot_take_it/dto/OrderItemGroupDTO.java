package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderItemGroupDTO {
    private final OrderItem firstItem;
    private final List<OrderItem> orderItems;
    private int totalQuantity;

    public OrderItemGroupDTO(OrderItem orderItem) {
        this.firstItem = orderItem;
        this.orderItems = new ArrayList<>();
        this.orderItems.add(orderItem);
        this.totalQuantity = orderItem.getQuantity();
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        this.totalQuantity += orderItem.getQuantity();
    }

    public Long getProductId() {
        return firstItem.getProduct().getId();
    }

    public String getProductName() {
        return firstItem.getProductName();
    }
}
