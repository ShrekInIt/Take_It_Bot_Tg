package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemAddonRepository cartItemAddonRepository;
    private final ProductService productService;
    private final UserService userService;

    /**
     * Получить корзину пользователя
     */
    @Transactional(readOnly = true)
    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> createCartForUser(user));
    }

    /**
     * Найти конкретный товар в корзине пользователя
     */
    @Transactional(readOnly = true)
    public List<CartItem> findCartItemByProduct(Long chatId, String name) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Cart cart = getCartByUser(user);

            // Ищем товар по productId в корзине
            List<CartItem> cartItems = cartItemRepository.findByCartIdAndProductName(cart.getId(), name);

            if (cartItems.isEmpty()) {
                return null;
            }

            return cartItems;

        } catch (Exception e) {
            log.error("Ошибка поиска товара в корзине: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Создать корзину для пользователя
     */
    @Transactional
    public Cart createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();

        Cart savedCart = cartRepository.save(cart);
        log.info("Создана корзина для пользователя: {} (ID: {})", user.getName(), user.getId());

        return savedCart;
    }

    /**
     * Добавить товар в корзину
     */
    @Transactional
    public List<CartItem> addProductToCart(Long chatId, Long productId, Integer quantity) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Cart cart = getCartByUser(user);
        List<CartItem> createdItems = new ArrayList<>();

        boolean isCoffee = product.getCategoryId() == 3;

        if (!isCoffee) {
            List<CartItem> existingItems = cartItemRepository.findByCartAndProduct(cart, product);
            CartItem existingItem = existingItems.isEmpty() ? null : existingItems.getFirst();

            if (existingItem != null) {
                existingItem.setCountProduct(existingItem.getCountProduct() + quantity);
                cartItemRepository.save(existingItem);
                createdItems.add(existingItem);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .countProduct(quantity)
                        .build();

                cart.addItem(newItem);
                CartItem savedItem = cartItemRepository.save(newItem);
                createdItems.add(savedItem);
            }
        } else {
            for (int i = 0; i < quantity; i++) {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .countProduct(1)
                        .build();

                cart.addItem(newItem);
                CartItem savedItem = cartItemRepository.save(newItem);
                createdItems.add(savedItem);

                log.info("Добавлен товар в корзину: {} (пользователь: {})",
                        product.getName(), user.getName());
            }
        }

        return createdItems;
    }

    /**
     * Добавить товар в корзину с добавками
     */
    @Transactional
    public CartItem addProductWithAddonToCart(Long chatId, Long productId, Integer quantity,
                                              Long addonProductId, Integer addonPrice) {
        List<CartItem> cartItems = addProductToCart(chatId, productId, quantity);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Не удалось добавить товар в корзину");
        }

        CartItem cartItem = cartItems.getFirst();

        if (addonProductId != null) {
            Product addonProduct = productService.getProductById(addonProductId)
                    .orElseThrow(() -> new IllegalArgumentException("Добавка не найдена"));

            CartItemAddon existingAddon = cartItemAddonRepository
                    .findByCartItemAndAddonProduct(cartItem, addonProduct).orElse(null);

            if (existingAddon != null) {
                existingAddon.setQuantity(existingAddon.getQuantity() + quantity);
                cartItemAddonRepository.save(existingAddon);
            } else {
                CartItemAddon newAddon = CartItemAddon.builder()
                        .cartItem(cartItem)
                        .addonProduct(addonProduct)
                        .quantity(quantity)
                        .priceAtSelection(addonPrice)
                        .build();

                cartItem.addAddon(newAddon);
                cartItemAddonRepository.save(newAddon);
                log.info("Добавлена добавка к товару: {} (цена: {})",
                        addonProduct.getName(), addonPrice);
            }
        }

        return cartItem;
    }

    /**
     * Очистить корзину
     */
    @Transactional
    public void clearCart(Long chatId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);
        cartItemRepository.deleteByCart(cart);
        log.info("Корзина очищена (пользователь: {})", user.getName());
    }

    /**
     * Получить содержимое корзины
     */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long chatId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);
        return cartItemRepository.findByCartWithProductAndAddons(cart);
    }


    /**
     * Получить общую сумму корзины
     */
    @Transactional(readOnly = true)
    public Integer getCartTotal(Long chatId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);
        return cart.calculateTotalAmount();
    }

    /**
     * Проверить, пуста ли корзина
     */
    @Transactional(readOnly = true)
    public boolean isCartEmpty(Long chatId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);
        return cart.isEmpty();
    }

    /**
     * Получить описание корзины в виде текста
     */
    @Transactional(readOnly = true)
    public String getCartDescription(Long chatId) {
        List<CartItem> items = getCartItems(chatId);

        if (items.isEmpty()) {
            return "🛒 Корзина пуста";
        }

        StringBuilder description = new StringBuilder();
        description.append("🛒 *Ваша корзина:*\n\n");

        int totalAmount = 0;

        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            int itemTotal = item.calculateItemTotal();
            totalAmount += itemTotal;

            description.append(i + 1).append(". *").append(item.getProduct().getName())
                    .append("* x").append(item.getCountProduct())
                    .append(" - ").append(itemTotal).append("₽\n");

            if (item.hasAddons()) {
                for (CartItemAddon addon : item.getAddons()) {
                    description.append("   🍯 ").append(addon.getAddonProduct().getName())
                            .append(" x").append(addon.getQuantity())
                            .append(" (+").append(addon.calculateAddonTotal()).append("₽)\n");
                }
            }

            if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isEmpty()) {
                description.append("   💬 ").append(item.getSpecialInstructions()).append("\n");
            }

            description.append("\n");
        }

        description.append("-------------------\n");
        description.append("💰 *Итого:* ").append(totalAmount).append("₽");

        return description.toString();
    }

    /**
     * Проверить если данный товар в корзине
     */
    @Transactional(readOnly = true)
    public boolean findProductInCart(Long chatId, Long productId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);

        List<CartItem> products = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        return products.isEmpty();
    }
}