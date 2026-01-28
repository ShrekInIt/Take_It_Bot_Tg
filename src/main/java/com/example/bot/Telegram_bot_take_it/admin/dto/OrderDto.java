package com.example.bot.Telegram_bot_take_it.admin.dto;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderDto {
    private Long id;

    private LocalDateTime createdAt;

    private String status;

    private Integer totalAmount;

    private String deliveryType;

    private String deliveryAddress;

    private String comment;

    private UserDto user;

    private List<OrderItemDto> items;

    public OrderDto(Long id, Order.OrderStatus status, Integer totalAmount, String userName, String userPhone, Order.DeliveryType deliveryType, LocalDateTime createdAt, String comment) {
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

    public OrderDto() {
    }
}
