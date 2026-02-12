package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.CartItemGroup;
import com.example.bot.Telegram_bot_take_it.dto.CartItemGroupDTO;
import com.example.bot.Telegram_bot_take_it.dto.OrderItemGroupDTO;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.CartRepository;
import com.example.bot.Telegram_bot_take_it.utils.CartSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserService userService;
    private final CartItemAddonService cartItemAddonService;
    private final SyrupPriceService syrupPriceService;
    private final CategoryService categoryService;
    private final CartItemAddonRepository cartItemAddonRepository;

    /**
     * Получить корзину пользователя
     */
    @Transactional(readOnly = true)
    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> createCartForUser(user));
    }

    /**
     * Получить группу по ID первого элемента
     */
    @Transactional(readOnly = true)
    public CartItemGroupDTO getItemGroupByFirstItemId(Long chatId, Long firstCartItemId) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Cart cart = getCartByUser(user);

            CartItem firstItem = cartItemRepository.findById(firstCartItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            if (!firstItem.getCart().getId().equals(cart.getId())) {
                throw new IllegalArgumentException("Товар не принадлежит корзине пользователя");
            }

            List<CartItem> allItems = cartItemRepository.findByCartWithProductAndAddons(cart);

            boolean isCoffee = productService.isCoffeeProduct(firstItem.getProduct().getId());

            List<CartItem> groupItems = new ArrayList<>();
            int totalQuantity = 0;

            for (CartItem item : allItems) {
                if (!item.getProduct().getId().equals(firstItem.getProduct().getId())) {
                    continue;
                }

                Set<Long> itemAddonIds = item.getAddons().stream()
                        .map(addon -> addon.getAddonProduct().getId())
                        .collect(Collectors.toSet());

                Set<Long> firstItemAddonIds = firstItem.getAddons().stream()
                        .map(addon -> addon.getAddonProduct().getId())
                        .collect(Collectors.toSet());

                if (!itemAddonIds.equals(firstItemAddonIds)) {
                    continue;
                }

                groupItems.add(item);

                totalQuantity += item.getCountProduct();
            }

            groupItems.sort(Comparator.comparing(CartItem::getId));

            log.info("Найдена группа товаров: продукт={}, записей={}, общее количество={}, кофе={}",
                    firstItem.getProduct().getName(), groupItems.size(), totalQuantity, isCoffee);

            return new CartItemGroupDTO(
                    firstItem.getProduct(),
                    groupItems,
                    firstItem.getAddons(),
                    totalQuantity,
                    isCoffee
            );

        } catch (Exception e) {
            log.error("Ошибка при получении группы товаров: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Удалить элемент корзины
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
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

    /**
     * Добавляет товар в корзину.
     */
    @Transactional
    public List<CartItem> addProductToCart(Long chatId, Long productId, Integer quantity) {
        User user = userService.getUserByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Cart cart = getCartByUser(user);
        List<CartItem> createdItems = new ArrayList<>();

        boolean isCoffee = categoryService.isCoffeeCategoryById(product.getCategory().getId());

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
     * Добавляет товар с add-on в корзину.
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
     * Очищает корзину пользователя.
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
     * Повторить заказ - добавить все товары из заказа в корзину
     */
    @Transactional
    public void repeatOrder(Long chatId, Order order) {
        try {
            clearCart(chatId);

            Map<String, OrderItemGroupDTO> groupedItems = new HashMap<>();

            for (OrderItem orderItem : order.getItems()) {
                String groupKey = generateGroupKey(orderItem);

                if (groupedItems.containsKey(groupKey)) {
                    OrderItemGroupDTO group = groupedItems.get(groupKey);
                    group.addOrderItem(orderItem);
                } else {
                    groupedItems.put(groupKey, new OrderItemGroupDTO(orderItem));
                }
            }

            for (OrderItemGroupDTO group : groupedItems.values()) {
                OrderItem firstItem = group.getOrderItems().getFirst();
                Product product = firstItem.getProduct();

                if (!product.getAvailable() || product.getCount() < group.getTotalQuantity()) {
                    throw new IllegalArgumentException(
                            String.format("Товар '%s' недоступен в нужном количестве. Доступно: %d",
                                    product.getName(), product.getCount()));
                }

                List<CartItem> cartItems = addProductToCart(chatId, product.getId(), group.getTotalQuantity());

                if (!firstItem.getAddons().isEmpty() && !cartItems.isEmpty()) {
                    for (CartItem cartItem : cartItems) {
                        for (OrderItemAddon orderItemAddon : firstItem.getAddons()) {
                            Product addonProduct = orderItemAddon.getAddonProduct();

                            if (addonProduct != null && addonProduct.getAvailable()) {
                                cartItemAddonService.addAddonToCartItem(
                                        cartItem,
                                        addonProduct,
                                        orderItemAddon.getQuantity(),
                                        orderItemAddon.getPriceAtOrder()
                                );
                            }
                        }
                    }
                }
            }
            log.info("Заказ повторен: orderId={}, chatId={}, товаров: {}",
                    order.getId(), chatId, groupedItems.size());

        } catch (Exception e) {
            log.error("Ошибка при повторении заказа: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Генерация ключа для группировки OrderItem
     */
    private String generateGroupKey(OrderItem orderItem) {
        StringBuilder key = new StringBuilder();
        key.append(orderItem.getProduct().getId());

        if (orderItem.getAddons() != null && !orderItem.getAddons().isEmpty()) {
            String addonsKey = orderItem.getAddons().stream()
                    .map(addon -> addon.getAddonProduct() != null ?
                            String.valueOf(addon.getAddonProduct().getId()) : "")
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining(","));

            if (!addonsKey.isEmpty()) {
                key.append("_").append(addonsKey);
            }
        }

        return key.toString();
    }

    /**
     * Получить описание корзины в виде текста с группировкой одинаковых позиций
     */
    @Transactional(readOnly = true)
    public String getCartDescription(Long chatId) {
        List<CartItem> items = getCartItems(chatId);

        if (items.isEmpty()) {
            return "🛒 Корзина пуста";
        }

        Map<String, CartItemGroup> groupedItems = new LinkedHashMap<>();

        for (CartItem item : items) {
            String groupKey = createGroupKey(item);

            CartItemGroup group = groupedItems.get(groupKey);
            if (group == null) {
                group = new CartItemGroup(item);
                groupedItems.put(groupKey, group);
            } else {
                group.addItem(item);
            }
        }

        StringBuilder description = new StringBuilder();
        description.append("🛒 *Ваша корзина:*\n\n");

        int totalAmount = 0;
        int itemNumber = 1;

        for (CartItemGroup group : groupedItems.values()) {
            CartItem firstItem = group.getFirstItem();
            int groupQuantity = group.getTotalQuantity();

            int itemTotal = firstItem.getProduct().getAmount() * groupQuantity;
            int addonsTotal = 0;

            List<String> addonDescriptions = new ArrayList<>();

            if (firstItem.hasAddons()) {
                for (CartItemAddon addon : firstItem.getAddons()) {
                    int syrupPricePerUnit;

                    if (syrupPriceService.isSyrup(addon.getAddonProduct())) {
                        syrupPricePerUnit = syrupPriceService.calculateSyrupPriceForSize(
                                addon.getAddonProduct(),
                                firstItem.getProduct()
                        );
                    } else {
                        syrupPricePerUnit = addon.getPriceAtSelection();
                    }

                    int addonQuantity = addon.getQuantity() * groupQuantity;
                    int addonTotalPrice = syrupPricePerUnit * addonQuantity;
                    addonsTotal += addonTotalPrice;

                    addonDescriptions.add(String.format(
                            "   🍯 %s x%d (+%d₽)\n",
                            addon.getAddonProduct().getName(),
                            addonQuantity,
                            addonTotalPrice
                    ));
                }
            }

            int groupTotalPrice = itemTotal + addonsTotal;
            totalAmount += groupTotalPrice;

            description.append(itemNumber).append(". *").append(firstItem.getProduct().getName())
                    .append("* x").append(groupQuantity)
                    .append(" - ").append(groupTotalPrice).append("₽\n");

            for (String addonDesc : addonDescriptions) {
                description.append(addonDesc);
            }

            if (firstItem.getSpecialInstructions() != null && !firstItem.getSpecialInstructions().isEmpty()) {
                description.append("   💬 ").append(firstItem.getSpecialInstructions()).append("\n");
            }

            description.append("\n");
            itemNumber++;
        }

        description.append("-------------------\n");
        description.append("💰 *Итого:* ").append(totalAmount).append("₽");

        return description.toString();
    }

    /**
     * Создает ключ для группировки элементов корзины
     * Ключ учитывает: product_id, special_instructions и все добавки
     */
    private String createGroupKey(CartItem item) {
        StringBuilder key = new StringBuilder();

        key.append("product:").append(item.getProduct().getId()).append("|");

        String instructions = item.getSpecialInstructions();
        key.append("instructions:").append(instructions != null ? instructions.hashCode() : "null").append("|");

        if (item.hasAddons()) {
            List<CartItemAddon> sortedAddons = item.getAddons().stream()
                    .sorted(Comparator.comparing(a -> a.getAddonProduct().getId()))
                    .toList();

            key.append("addons:");
            for (CartItemAddon addon : sortedAddons) {
                key.append(addon.getAddonProduct().getId())
                        .append(":")
                        .append(addon.getQuantity())
                        .append(",");
            }
        } else {
            key.append("addons:none");
        }

        return key.toString();
    }

    @Transactional(readOnly = true)
    public CartSnapshot getCartSnapshot(Long chatId) {
        User user = userService.getUserByChatId(chatId).orElseThrow();
        Cart cart = getCartByUser(user);
        List<CartItem> items = cartItemRepository.findByCartWithProductAndAddons(cart);
        int total = cart.calculateTotalAmount();
        return new CartSnapshot(cart, items, total);
    }
}