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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
     * Получить товар в корзине по ID с проверкой принадлежности пользователю
     * Безопасный метод - проверяет, что товар принадлежит пользователю
     */
    @Transactional(readOnly = true)
    public CartItem getCartItemByIdWithUserCheck(Long chatId, Long cartItemId) {
        try {
            log.info("Получение товара из корзины: chatId={}, cartItemId={}", chatId, cartItemId);

            // 1. Получаем пользователя по chatId
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> {
                        log.warn("Пользователь с chatId {} не найден", chatId);
                        return new RuntimeException("Пользователь не найден");
                    });

            // 2. Получаем корзину пользователя
            Cart cart = cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> {
                        log.warn("Корзина для пользователя {} не найдена", user.getId());
                        return new RuntimeException("Корзина не найдена");
                    });

            // 3. Ищем товар в корзине по ID
            Optional<CartItem> cartItemOpt = cartItemRepository.findById(cartItemId);

            if (cartItemOpt.isEmpty()) {
                log.warn("CartItem с ID {} не найден", cartItemId);
                throw new RuntimeException("Товар в корзине не найден");
            }

            CartItem cartItem = cartItemOpt.get();

            // 4. Проверяем, что товар принадлежит корзине пользователя
            if (!cartItem.getCart().getId().equals(cart.getId())) {
                log.warn("Товар {} не принадлежит пользователю {}", cartItemId, user.getId());
                throw new RuntimeException("Товар не принадлежит пользователю");
            }

            // 5. Инициализируем lazy-поля (важно для избежания LazyInitializationException)
            cartItem.getProduct().getName(); // Загружаем продукт
            if (cartItem.getAddons() != null) {
                cartItem.getAddons().size(); // Загружаем добавки если есть
            }

            log.info("Товар в корзине найден: id={}, product={}, quantity={}",
                    cartItem.getId(), cartItem.getProduct().getName(), cartItem.getCountProduct());

            return cartItem;

        } catch (Exception e) {
            log.error("Ошибка получения товара из корзины: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить товар из корзины: " + e.getMessage());
        }
    }

    /**
     * Получить корзину по chatId пользователя
     */
    @Transactional(readOnly = true)
    public Cart getCartByChatId(Long chatId) {
        return userService.getUserByChatId(chatId)
                .map(this::getCartByUser)
                .orElse(null);
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

        // Проверяем, является ли товар кофе (категория 3)
        boolean isCoffee = product.getCategoryId() == 3;

        if (!isCoffee) {
            // Для НЕ кофе: ищем существующий товар и увеличиваем количество
            List<CartItem> existingItems = cartItemRepository.findByCartAndProduct(cart, product);
            CartItem existingItem = existingItems.isEmpty() ? null : existingItems.get(0);

            if (existingItem != null) {
                // Увеличиваем количество существующего товара
                existingItem.setCountProduct(existingItem.getCountProduct() + quantity);
                cartItemRepository.save(existingItem);
                createdItems.add(existingItem);
            } else {
                // Создаем новый товар в корзине для некофе
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
            // Для кофе: создаем quantity отдельных позиций с countProduct = 1
            for (int i = 0; i < quantity; i++) {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .countProduct(1) // Каждый кофе отдельно
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

        // Берем первый добавленный товар для добавления добавки
        CartItem cartItem = cartItems.get(0);

        if (addonProductId != null) {
            Product addonProduct = productService.getProductById(addonProductId)
                    .orElseThrow(() -> new IllegalArgumentException("Добавка не найдена"));

            // Проверяем, есть ли уже такая добавка у товара
            CartItemAddon existingAddon = cartItemAddonRepository
                    .findByCartItemAndAddonProduct(cartItem, addonProduct).orElse(null);

            if (existingAddon != null) {
                // Увеличиваем количество существующей добавки
                existingAddon.setQuantity(existingAddon.getQuantity() + quantity);
                cartItemAddonRepository.save(existingAddon);
            } else {
                // Создаем новую добавку
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
     * Обновить количество товара в корзине
     */
    @Transactional
    public CartItem updateProductQuantity(Long chatId, Long cartItemId, Integer newQuantity) {
        if (newQuantity <= 0) {
            // Удаляем товар из корзины
            CartItem item = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
            cartItemRepository.delete(item);
            return null;
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        // Проверяем, является ли товар кофе
        boolean isCoffee = cartItem.getProduct().getCategoryId() == 3;

        if (isCoffee) {
            // Для кофе: просто устанавливаем количество
            cartItem.setCountProduct(newQuantity);
        } else {
            // Для других товаров - увеличиваем/уменьшаем количество
            cartItem.setCountProduct(newQuantity);
        }

        return cartItemRepository.save(cartItem);
    }

    /**
     * Удалить товар из корзины
     */
    @Transactional
    public void removeProductFromCart(Long chatId, Long productId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Cart cart = getCartByUser(user);

        // Получаем все позиции с этим товаром
        List<CartItem> cartItems = cartItemRepository.findByCartAndProduct(cart, product);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Товар не найден в корзине");
        }

        // Удаляем все позиции с этим товаром
        cartItemRepository.deleteAll(cartItems);
        log.info("Удален товар из корзины: {} (пользователь: {})",
                product.getName(), user.getName());
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
        // Используем метод с JOIN FETCH вместо обычного
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
     * Получить количество товаров в корзине
     */
    @Transactional(readOnly = true)
    public Integer getCartItemsCount(Long chatId) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Cart cart = getCartByUser(user);
        return cart.getTotalItemsCount();
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

        // Используем метод, возвращающий список
        List<CartItem> products = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        // Возвращаем true, если есть хотя бы один товар в корзине
        return products.isEmpty();
    }

}