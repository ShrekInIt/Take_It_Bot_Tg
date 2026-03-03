package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.CartItemGroup;
import com.example.bot.Telegram_bot_take_it.dto.CartItemGroupDTO;
import com.example.bot.Telegram_bot_take_it.dto.response.*;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.mapper.CartItemMapper;
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
    private final CartItemMapper cartItemMapper;

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

    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getCartItemsDto(Long chatId) {
        return cartItemMapper.toResponseDtos(getCartItems(chatId));
    }

    /**
     * Получить общую сумму корзины
     */
    @Transactional(readOnly = true)
    public Long getCartTotal(Long chatId) {
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
                                              Long addonProductId, Long addonPrice) {
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
     * Повторить заказ - добавить все товары из заказа в корзину (DTO)
     */
    @Transactional
    public void repeatOrder(Long chatId, OrderResponseDto orderDto) {
        try {
            if (orderDto == null || orderDto.getItems() == null || orderDto.getItems().isEmpty()) {
                throw new IllegalArgumentException("Заказ не содержит товаров");
            }

            clearCart(chatId);

            Map<String, OrderItemDtoGroup> groupedItems = new HashMap<>();

            for (OrderItemResponseDto orderItem : orderDto.getItems()) {
                String groupKey = generateGroupKey(orderItem);

                OrderItemDtoGroup group = groupedItems.get(groupKey);
                if (group == null) {
                    groupedItems.put(groupKey, new OrderItemDtoGroup(orderItem));
                } else {
                    group.add(orderItem);
                }
            }

            for (OrderItemDtoGroup group : groupedItems.values()) {
                OrderItemResponseDto firstItem = group.getFirstItem();
                Long productId = firstItem.getProductId();
                if (productId == null) {
                    throw new IllegalArgumentException("Товар не найден");
                }

                Product product = productService.getProductById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

                if (!product.getAvailable() || product.getCount() < group.getTotalQuantity()) {
                    throw new IllegalArgumentException(
                            String.format("Товар '%s' недоступен в нужном количестве. Доступно: %d",
                                    product.getName(), product.getCount()));
                }

                List<CartItem> cartItems = addProductToCart(chatId, product.getId(), group.getTotalQuantity());

                if (firstItem.getAddons() != null && !firstItem.getAddons().isEmpty() && !cartItems.isEmpty()) {
                    for (CartItem cartItem : cartItems) {
                        for (OrderItemAddonResponseDto addonDto : firstItem.getAddons()) {
                            Long addonProductId = addonDto.getAddonProductId();
                            if (addonProductId == null) {
                                continue;
                            }

                            Product addonProduct = productService.getProductById(addonProductId)
                                    .orElse(null);

                            if (addonProduct != null && addonProduct.getAvailable()) {
                                cartItemAddonService.addAddonToCartItem(
                                        cartItem,
                                        addonProduct,
                                        addonDto.getQuantity(),
                                        addonDto.getPriceAtOrder()
                                );
                            }
                        }
                    }
                }
            }

            log.info("Заказ повторен (DTO): orderId={}, chatId={}, товаров: {}",
                    orderDto.getId(), chatId, groupedItems.size());

        } catch (Exception e) {
            log.error("Ошибка при повторении заказа (DTO): {}", e.getMessage(), e);
            throw e;
        }
    }

    private static class OrderItemDtoGroup {
        private final OrderItemResponseDto firstItem;
        private int totalQuantity;

        private OrderItemDtoGroup(OrderItemResponseDto firstItem) {
            this.firstItem = firstItem;
            this.totalQuantity = safeQuantity(firstItem);
        }

        private void add(OrderItemResponseDto item) {
            this.totalQuantity += safeQuantity(item);
        }

        private OrderItemResponseDto getFirstItem() {
            return firstItem;
        }

        private int getTotalQuantity() {
            return totalQuantity;
        }

        private static int safeQuantity(OrderItemResponseDto item) {
            return item != null && item.getQuantity() != null ? item.getQuantity() : 0;
        }
    }

    /**
     * Генерация ключа для группировки OrderItem (DTO)
     */
    private String generateGroupKey(OrderItemResponseDto orderItem) {
        StringBuilder key = new StringBuilder();
        key.append(orderItem.getProductId());

        if (orderItem.getAddons() != null && !orderItem.getAddons().isEmpty()) {
            String addonsKey = orderItem.getAddons().stream()
                    .map(addon -> addon.getAddonProductId() != null ?
                            String.valueOf(addon.getAddonProductId()) : "")
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

        long totalAmount = 0;
        int itemNumber = 1;

        for (CartItemGroup group : groupedItems.values()) {
            CartItem firstItem = group.getFirstItem();
            int groupQuantity = group.getTotalQuantity();

            long itemTotal = firstItem.getProduct().getAmount() * groupQuantity;
            long addonsTotal = 0;

            List<String> addonDescriptions = new ArrayList<>();

            if (firstItem.hasAddons()) {
                for (CartItemAddon addon : firstItem.getAddons()) {
                    long syrupPricePerUnit;

                    if (syrupPriceService.isSyrup(addon.getAddonProduct())) {
                        syrupPricePerUnit = syrupPriceService.calculateSyrupPriceForSize(
                                addon.getAddonProduct(),
                                firstItem.getProduct()
                        );
                    } else {
                        syrupPricePerUnit = addon.getPriceAtSelection();
                    }

                    int addonQuantity = addon.getQuantity() * groupQuantity;
                    long addonTotalPrice = syrupPricePerUnit * addonQuantity;
                    addonsTotal += addonTotalPrice;

                    addonDescriptions.add(String.format(
                            "   🍯 %s x%d (+%d₽)\n",
                            addon.getAddonProduct().getName(),
                            addonQuantity,
                            addonTotalPrice
                    ));
                }
            }

            long groupTotalPrice = itemTotal + addonsTotal;
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
        long total = cart.calculateTotalAmount();
        return new CartSnapshot(cart, items, total);
    }

    @Transactional(readOnly = true)
    public CartItemGroupResponseDto getItemGroupByFirstItemIdDto(Long chatId, Long firstCartItemId) {
        CartItemGroupDTO group = getItemGroupByFirstItemId(chatId, firstCartItemId);
        if (group == null) {
            return null;
        }

        return cartItemMapper.toGroupResponseDto(group.getProduct(), group.getItems());
    }

    @Transactional(readOnly = true)
    public List<CartItemResponseDto> findCartItemByProductDto(Long chatId, String name) {
        List<CartItem> items = findCartItemByProduct(chatId, name);
        return cartItemMapper.toResponseDtos(items);
    }

    @Transactional
    public List<CartItemResponseDto> addProductToCartDto(Long chatId, Long productId, Integer quantity) {
        return cartItemMapper.toResponseDtos(addProductToCart(chatId, productId, quantity));
    }

    @Transactional
    public CartItemResponseDto addProductWithAddonToCartDto(Long chatId, Long productId, Integer quantity,
                                                            Long addonProductId, Long addonPrice) {
        return cartItemMapper.toResponseDto(addProductWithAddonToCart(chatId, productId, quantity, addonProductId, addonPrice));
    }

    @Transactional
    public void updateCartItemQuantity(Long cartItemId, int newQuantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар в корзине не найден"));
        item.setCountProduct(newQuantity);
        cartItemRepository.save(item);
    }
}
