package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemAddonService {

    public static final Long SYRUP_CATEGORY_ID = 20L;
    public static final Long MILK_CATEGORY_ID = 21L;

    private final CartItemAddonRepository cartItemAddonRepository;

    /**
     * Получить сироп для cart_item (категория 20)
     */
    public Product getSyrupByCartItemId(Long cartItemId) {
        log.debug("Getting syrup for cartItemId: {}", cartItemId);
        List<Product> syrups = cartItemAddonRepository
                .findAddonProductsByCartItemIdAndCategoryId(cartItemId, SYRUP_CATEGORY_ID);

        // Возвращаем первый сироп или null
        return syrups.isEmpty() ? null : syrups.getFirst();
    }

    /**
     * Получить молоко для cart_item (категория 21 или другая)
     */
    public Product getMilkByCartItemId(Long cartItemId) {
        log.debug("Getting milk for cartItemId: {}", cartItemId);
        List<Product> milks = cartItemAddonRepository
                .findAddonProductsByCartItemIdAndCategoryId(cartItemId, MILK_CATEGORY_ID);

        // Возвращаем первое молоко или null
        return milks.isEmpty() ? null : milks.getFirst();
    }

    /**
     * Получить все добавки для cart_item по его ID
     */
    public List<CartItemAddon> getAllAddonsByCartItemId(Long cartItemId) {
        log.debug("Getting all addons for cartItemId: {}", cartItemId);
        return cartItemAddonRepository.findByCartItemId(cartItemId);
    }

    /**
     * Получить все добавки для cart_item по его ID с загруженными продуктами
     */
    public List<CartItemAddon> getAllAddonsByCartItemIdWithProducts(Long cartItemId) {
        log.debug("Getting all addons with products for cartItemId: {}", cartItemId);
        return cartItemAddonRepository.findByCartItemIdWithProducts(cartItemId);
    }

    /**
     * Получить все продукты-добавки для cart_item по его ID
     */
    public List<Product> getAllAddonProductsByCartItemId(Long cartItemId) {
        log.debug("Getting all addon products for cartItemId: {}", cartItemId);
        return cartItemAddonRepository.findAddonProductsByCartItemId(cartItemId);
    }

    /**
     * Получить все ID продуктов-добавок для cart_item
     */
    public List<Long> getAllAddonProductIdsByCartItemId(Long cartItemId) {
        log.debug("Getting all addon product IDs for cartItemId: {}", cartItemId);
        return cartItemAddonRepository.findAddonProductIdsByCartItemId(cartItemId);
    }

    /**
     * Получить количество добавок для cart_item
     */
    public Integer getAddonsCountByCartItemId(Long cartItemId) {
        log.debug("Getting addons count for cartItemId: {}", cartItemId);
        return cartItemAddonRepository.countByCartItemId(cartItemId);
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
     * Получить общую стоимость всех добавок для cart_item
     */
    public Integer calculateTotalAddonsPriceForCartItem(Long cartItemId) {
        log.debug("Calculating total addons price for cartItemId: {}", cartItemId);
        Integer total = cartItemAddonRepository.calculateTotalAddonsPriceForCartItem(cartItemId);
        return total != null ? total : 0;
    }

    /**
     * Получить добавки для cart_item в виде Map (Product → количество)
     */
    public Map<Product, Integer> getAddonsMapByCartItemId(Long cartItemId) {
        log.debug("Getting addons map for cartItemId: {}", cartItemId);
        List<CartItemAddon> addons = cartItemAddonRepository.findByCartItemIdWithProducts(cartItemId);

        return addons.stream()
                .collect(Collectors.toMap(
                        CartItemAddon::getAddonProduct,
                        CartItemAddon::getQuantity
                ));
    }

    /**
     * Получить добавку по ID cart_item и ID продукта-добавки
     */
    public CartItemAddon getAddonByCartItemIdAndProductId(Long cartItemId, Long addonProductId) {
        log.debug("Getting addon for cartItemId: {} and productId: {}", cartItemId, addonProductId);
        return cartItemAddonRepository.findByCartItemIdAndAddonProductId(cartItemId, addonProductId)
                .orElse(null);
    }

    /**
     * Добавить добавку к cart_item
     */
    @Transactional
    public CartItemAddon addAddonToCartItem(CartItem cartItem, Product addonProduct, Integer quantity, Integer priceAtSelection) {
        log.debug("Adding addon productId: {} to cartItemId: {} with quantity: {} and price: {}",
                addonProduct.getId(), cartItem.getId(), quantity, priceAtSelection);

        // Проверяем, нет ли уже такой добавки
        Optional<CartItemAddon> existingAddon = cartItemAddonRepository
                .findByCartItemIdAndAddonProductId(cartItem.getId(), addonProduct.getId());

        if (existingAddon.isPresent()) {
            // Обновляем количество существующей добавки
            CartItemAddon addon = existingAddon.get();
            addon.setQuantity(addon.getQuantity() + quantity);
            return cartItemAddonRepository.save(addon);
        } else {
            // Создаем новую добавку
            CartItemAddon cartItemAddon = new CartItemAddon();
            cartItemAddon.setCartItem(cartItem);
            cartItemAddon.setAddonProduct(addonProduct);
            cartItemAddon.setQuantity(quantity);
            cartItemAddon.setPriceAtSelection(priceAtSelection);
            return cartItemAddonRepository.save(cartItemAddon);
        }
    }

    /**
     * Обновить количество добавки
     */
    @Transactional
    public CartItemAddon updateAddonQuantity(Long cartItemId, Long addonProductId, Integer newQuantity) {
        log.debug("Updating addon quantity for cartItemId: {}, productId: {} to {}",
                cartItemId, addonProductId, newQuantity);

        CartItemAddon addon = cartItemAddonRepository
                .findByCartItemIdAndAddonProductId(cartItemId, addonProductId)
                .orElseThrow(() -> new RuntimeException("Addon not found"));

        addon.setQuantity(newQuantity);
        return cartItemAddonRepository.save(addon);
    }

    /**
     * Удалить все добавки для cart_item
     */
    @Transactional
    public void deleteAllAddonsByCartItemId(Long cartItemId) {
        log.debug("Deleting all addons for cartItemId: {}", cartItemId);
        cartItemAddonRepository.deleteByCartItemId(cartItemId);
    }

    /**
     * Удалить конкретную добавку для cart_item
     */
    @Transactional
    public void deleteAddonByCartItemIdAndProductId(Long cartItemId, Long addonProductId) {
        log.debug("Deleting addon for cartItemId: {} and productId: {}", cartItemId, addonProductId);
        cartItemAddonRepository.deleteByCartItemIdAndAddonProductId(cartItemId, addonProductId);
    }

    /**
     * Получить сводную информацию о добавках для cart_item
     */
    public String getAddonsSummary(Long cartItemId) {
        List<CartItemAddon> addons = cartItemAddonRepository.findByCartItemIdWithProducts(cartItemId);

        if (addons.isEmpty()) {
            return "Без добавок";
        }

        return addons.stream()
                .map(addon -> String.format("%s x%d (+%d ₽)",
                        addon.getAddonProduct().getName(),
                        addon.getQuantity(),
                        addon.getPriceAtSelection() * addon.getQuantity()))
                .collect(Collectors.joining(", "));
    }
}
