package com.example.bot.Telegram_bot_take_it.mapper;

import com.example.bot.Telegram_bot_take_it.dto.response.CartItemAddonResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.CartItemGroupResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.CartItemResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartItemMapper {

    private final ProductMapper productMapper;

    public CartItemResponseDto toResponseDto(CartItem item) {
        if (item == null) {
            return null;
        }

        return CartItemResponseDto.builder()
                .id(item.getId())
                .product(productMapper.toResponseDto(item.getProduct()))
                .countProduct(item.getCountProduct())
                .specialInstructions(item.getSpecialInstructions())
                .addons(toAddonDtos(item.getAddons()))
                .build();
    }

    public List<CartItemResponseDto> toResponseDtos(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(this::toResponseDto)
                .toList();
    }

    public CartItemGroupResponseDto toGroupResponseDto(Product product, List<CartItem> items) {
        ProductResponseDto productDto = productMapper.toResponseDto(product);
        List<CartItemResponseDto> itemDtos = toResponseDtos(items);
        int totalQuantity = items == null ? 0 : items.stream().mapToInt(CartItem::getCountProduct).sum();
        boolean isCoffee = product != null && product.getCategory() != null
                && Long.valueOf(3L).equals(product.getCategory().getId());

        return CartItemGroupResponseDto.builder()
                .product(productDto)
                .items(itemDtos)
                .totalQuantity(totalQuantity)
                .isCoffee(isCoffee)
                .build();
    }

    private List<CartItemAddonResponseDto> toAddonDtos(List<CartItemAddon> addons) {
        if (addons == null || addons.isEmpty()) {
            return Collections.emptyList();
        }

        return addons.stream()
                .map(this::toAddonDto)
                .toList();
    }

    private CartItemAddonResponseDto toAddonDto(CartItemAddon addon) {
        if (addon == null) {
            return null;
        }

        return CartItemAddonResponseDto.builder()
                .id(addon.getId())
                .addonProductId(addon.getAddonProduct() != null ? addon.getAddonProduct().getId() : null)
                .addonProductName(addon.getAddonProduct() != null ? addon.getAddonProduct().getName() : null)
                .quantity(addon.getQuantity())
                .priceAtSelection(addon.getPriceAtSelection())
                .build();
    }
}

