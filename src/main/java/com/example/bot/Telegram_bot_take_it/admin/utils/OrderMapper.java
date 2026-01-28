package com.example.bot.Telegram_bot_take_it.admin.utils;

import com.example.bot.Telegram_bot_take_it.admin.dto.*;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.entity.OrderItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderDto toDto(Order order) {
        if (order == null) return null;

        OrderDto dto = new OrderDto();

        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name().toUpperCase());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDeliveryAddress(order.getAddress());
        dto.setComment(order.getComments());

        dto.setUser(toUserDto(order.getUser()));

        if (order.getItems() != null) {
            dto.setItems(
                    order.getItems()
                            .stream()
                            .map(this::toItemDto)
                            .toList()
            );
        } else {
            dto.setItems(List.of());
        }

        return dto;
    }

    private UserDto toUserDto(User user) {
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setId(user.getId());

        // В БД только name
        dto.setUsername(user.getName());

        dto.setTelegramId(user.getTelegramId());
        dto.setPhoneNumber(user.getPhoneNumber());

        return dto;
    }


    private OrderItemDto toItemDto(OrderItem item) {
        if (item == null) return null;

        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPriceAtOrder());

        ProductDto productDto = new ProductDto();
        productDto.setName(item.getProductName());
        dto.setProduct(productDto);

        if (item.getAddons() != null) {
            dto.setAddons(
                    item.getAddons()
                            .stream()
                            .map(this::toAddonDto)
                            .toList()
            );
        } else {
            dto.setAddons(List.of());
        }

        return dto;
    }


    private OrderItemAddonDto toAddonDto(OrderItemAddon addon) {
        if (addon == null) return null;

        OrderItemAddonDto dto = new OrderItemAddonDto();
        dto.setId(addon.getId());
        dto.setName(addon.getAddonProductName());
        dto.setQuantity(addon.getQuantity());
        dto.setPrice(addon.getPriceAtOrder());

        return dto;
    }
}
