package com.example.bot.Telegram_bot_take_it.mapper;

import com.example.bot.Telegram_bot_take_it.dto.response.OrderItemAddonResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.OrderItemResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.OrderResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.entity.OrderItemAddon;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper для преобразования Order Entity в OrderResponseDto.
 */
@Component
public class OrderResponseMapper {

    public OrderResponseDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .deliveryType(order.getDeliveryType() != null ? order.getDeliveryType().name() : null)
                .totalAmount(order.getTotalAmount())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .comments(order.getComments())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(toItemDtos(order.getItems()))
                .build();
    }

    private List<OrderItemResponseDto> toItemDtos(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(this::toItemDto)
                .toList();
    }

    private OrderItemResponseDto toItemDto(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .priceAtOrder(item.getPriceAtOrder())
                .addons(toAddonDtos(item.getAddons()))
                .build();
    }

    private List<OrderItemAddonResponseDto> toAddonDtos(List<OrderItemAddon> addons) {
        if (addons == null || addons.isEmpty()) {
            return Collections.emptyList();
        }

        return addons.stream()
                .map(this::toAddonDto)
                .toList();
    }

    private OrderItemAddonResponseDto toAddonDto(OrderItemAddon addon) {
        if (addon == null) {
            return null;
        }

        return OrderItemAddonResponseDto.builder()
                .id(addon.getId())
                .addonProductId(addon.getAddonProduct() != null ? addon.getAddonProduct().getId() : null)
                .addonProductName(addon.getAddonProductName())
                .quantity(addon.getQuantity())
                .priceAtOrder(addon.getPriceAtOrder())
                .build();
    }
}

