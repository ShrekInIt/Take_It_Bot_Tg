package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CartItem addProductToCart(Long chatId, Long productId, Integer quantity) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Cart cart = getCartByUser(user);

        // Проверяем, есть ли уже такой товар в корзине
        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);

        if (existingItem != null) {
            // Увеличиваем количество существующего товара
            existingItem.setCountProduct(existingItem.getCountProduct() + quantity);
            return cartItemRepository.save(existingItem);
        } else {
            // Создаем новый товар в корзине
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .countProduct(quantity)
                    .build();

            cart.addItem(newItem);
            CartItem savedItem = cartItemRepository.save(newItem);
            log.info("Добавлен товар в корзину: {} x{} (пользователь: {})",
                    product.getName(), quantity, user.getName());

            return savedItem;
        }
    }

    /**
     * Добавить товар в корзину с добавками
     */
    @Transactional
    public CartItem addProductWithAddonToCart(Long chatId, Long productId, Integer quantity,
                                              Long addonProductId, Integer addonPrice) {
        CartItem cartItem = addProductToCart(chatId, productId, quantity);

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
    public CartItem updateProductQuantity(Long chatId, Long productId, Integer newQuantity) {
        if (newQuantity <= 0) {
            removeProductFromCart(chatId, productId);
            return null;
        }

        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Cart cart = getCartByUser(user);

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        cartItem.setCountProduct(newQuantity);
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

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        cartItemRepository.delete(cartItem);
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

        Optional<CartItem> product = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
        return product.isEmpty();
    }
}