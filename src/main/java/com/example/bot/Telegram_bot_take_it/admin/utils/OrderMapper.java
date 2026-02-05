package com.example.bot.Telegram_bot_take_it.admin.utils;

import com.example.bot.Telegram_bot_take_it.admin.dto.*;
import com.example.bot.Telegram_bot_take_it.entity.*;
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

    public static AdminCategoryDto toDtoCategory(Category c) {
        return AdminCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .categoryTypeId(c.getCategoryType() != null ? c.getCategoryType().getId() : null)
                .categoryTypeName(c.getCategoryType() != null ? c.getCategoryType().getName() : null)
                .build();
    }

    public static AdminProductDto toDtoProduct(Product p) {
        return AdminProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .amount(p.getAmount())
                .available(p.getAvailable())
                .description(p.getDescription())
                .photo(p.getPhoto())
                .size(p.getSize())
                .count(p.getCount())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }


    public static AdminUserDto toDtoUser(User c) {
        return AdminUserDto.builder()
                .id(c.getId())
                .name(c.getName())
                .isActive(c.getIsActive())
                .telegramId(c.getTelegramId())
                .isAdmin(c.getIsAdmin())
                .chatId(c.getChatId())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
