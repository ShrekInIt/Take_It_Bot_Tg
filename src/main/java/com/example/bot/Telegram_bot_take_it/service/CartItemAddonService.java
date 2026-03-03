package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.example.bot.Telegram_bot_take_it.mapper.ProductMapper;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemAddonService {

    public static final Long SYRUP_CATEGORY_ID = 20L;
    public static final Long MILK_CATEGORY_ID = 21L;

    private final CartItemAddonRepository cartItemAddonRepository;
    private final ProductMapper productMapper;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    /**
     * Получить сироп для cart_item (категория 20)
     */
    @Transactional(readOnly = true)
    public Product getSyrupByCartItemId(Long cartItemId) {
        log.debug("Getting syrup for cartItemId: {}", cartItemId);
        List<Product> syrups = cartItemAddonRepository
                .findAddonProductsByCartItemIdAndCategoryId(cartItemId, SYRUP_CATEGORY_ID);

        return syrups.isEmpty() ? null : syrups.getFirst();
    }

    /**
     * Получить молоко для cart_item (категория 21 или другая)
     */
    @Transactional(readOnly = true)
    public Product getMilkByCartItemId(Long cartItemId) {
        log.debug("Getting milk for cartItemId: {}", cartItemId);
        List<Product> milks = cartItemAddonRepository
                .findAddonProductsByCartItemIdAndCategoryId(cartItemId, MILK_CATEGORY_ID);

        return milks.isEmpty() ? null : milks.getFirst();
    }

    /**
     * Проверить, есть ли добавки у cart_item
     */
    public boolean hasAddons(Long cartItemId) {
        log.debug("Checking if cartItemId: {} has addons", cartItemId);
        Boolean exists = cartItemAddonRepository.existsByCartItemId(cartItemId);
        return exists != null && exists;
    }

    /**
     * Добавить добавку к cart_item
     */
    @Transactional
    public void addAddonToCartItem(CartItem cartItem, Product addonProduct, Integer quantity, Long priceAtSelection) {
        log.debug("Adding addon productId: {} to cartItemId: {} with quantity: {} and price: {}",
                addonProduct.getId(), cartItem.getId(), quantity, priceAtSelection);

        Optional<CartItemAddon> existingAddon = cartItemAddonRepository
                .findByCartItemIdAndAddonProductId(cartItem.getId(), addonProduct.getId());

        if (existingAddon.isPresent()) {
            CartItemAddon addon = existingAddon.get();
            addon.setQuantity(addon.getQuantity() + quantity);
            cartItemAddonRepository.save(addon);
        } else {
            CartItemAddon cartItemAddon = new CartItemAddon();
            cartItemAddon.setCartItem(cartItem);
            cartItemAddon.setAddonProduct(addonProduct);
            cartItemAddon.setQuantity(quantity);
            cartItemAddon.setPriceAtSelection(priceAtSelection);
            cartItemAddonRepository.save(cartItemAddon);
        }
    }

    /**
     * Удалить конкретную добавку для cart_item
     */
    @Transactional
    public void deleteAddonByCartItemIdAndProductId(Long cartItemId, Long addonProductId) {
        log.debug("Deleting addon for cartItemId: {} and productId: {}", cartItemId, addonProductId);
        cartItemAddonRepository.deleteByCartItemIdAndAddonProductId(cartItemId, addonProductId);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getSyrupByCartItemIdDto(Long cartItemId) {
        return productMapper.toResponseDto(getSyrupByCartItemId(cartItemId));
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getMilkByCartItemIdDto(Long cartItemId) {
        return productMapper.toResponseDto(getMilkByCartItemId(cartItemId));
    }

    /**
     * Добавить добавку к cart_item по ID
     */
    @Transactional
    public void addAddonToCartItem(Long cartItemId, Long addonProductId, Integer quantity, Long priceAtSelection) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар в корзине не найден"));
        Product addonProduct = productService.getProductById(addonProductId)
                .orElseThrow(() -> new IllegalArgumentException("Добавка не найдена"));

        addAddonToCartItem(cartItem, addonProduct, quantity, priceAtSelection);
    }
}
