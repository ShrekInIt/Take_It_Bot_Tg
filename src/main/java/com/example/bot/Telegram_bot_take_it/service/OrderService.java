package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.dto.OrderRequest;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
import com.example.bot.Telegram_bot_take_it.utils.ConfectioneryBotClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemAddonRepository orderItemAddonRepository;
    private final UserService userService;
    private final CartService cartService;
    private final ProductService productService;
    private final ConfectioneryBotClient confectioneryBotClient;

    /**
     * Получить все заказы пользователя с загруженными items
     */
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long chatId) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            List<Order> orders = orderRepository.findByUserWithItems(user);

            for (Order order : orders) {
                List<OrderItem> itemsWithAddons = orderRepository.findOrderItemsWithAddons(order.getId());
                Map<Long, OrderItem> itemsMap = itemsWithAddons.stream()
                        .collect(Collectors.toMap(OrderItem::getId, item -> item));

                order.getItems().forEach(item -> {
                    OrderItem itemWithAddons = itemsMap.get(item.getId());
                    if (itemWithAddons != null) {
                        item.setAddons(itemWithAddons.getAddons());
                    }
                });
            }

            return orders;
        } catch (Exception e) {
            log.error("Ошибка при получении заказов пользователя: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Получить детали заказа по ID с загруженными items и addons
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByIdAndUser(Long orderId, Long chatId) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Optional<Order> orderOpt = orderRepository.findByIdAndUserWithItems(orderId, user);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                loadAddonsForOrder(order);
                return Optional.of(order);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Ошибка при получении заказа: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Вспомогательный метод для загрузки addons для заказа
     */
    private void loadAddonsForOrder(Order order) {
        List<OrderItem> itemsWithAddons = orderRepository.findOrderItemsWithAddons(order.getId());
        Map<Long, OrderItem> itemsMap = itemsWithAddons.stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        order.getItems().forEach(item -> {
            OrderItem itemWithAddons = itemsMap.get(item.getId());
            if (itemWithAddons != null) {
                item.setAddons(itemWithAddons.getAddons());
            }
        });
    }

    /**
     * Создание заказа из корзины
     */
    @Transactional
    public Order createOrderFromCart(Long chatId, OrderData orderData) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            List<CartItem> cartItems = cartService.getCartItems(chatId);

            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("Корзина пуста");
            }

            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                if (product.getCount() < cartItem.getCountProduct()) {
                    throw new IllegalArgumentException(
                            String.format("Товар '%s' недоступен в нужном количестве. Осталось: %d",
                                    product.getName(), product.getCount()));
                }
            }

            int totalAmount = cartService.getCartTotal(chatId);

            Order.OrderStatus status = Order.OrderStatus.PENDING;
            Order.DeliveryType deliveryType;
            try {
                deliveryType = Order.DeliveryType.valueOf(orderData.getDeliveryType());
            } catch (IllegalArgumentException e) {
                deliveryType = Order.DeliveryType.PICKUP;
            }

            Order order = Order.builder()
                    .user(user)
                    .totalAmount(totalAmount)
                    .status(status)
                    .deliveryType(deliveryType)
                    .phoneNumber(orderData.getPhoneNumber())
                    .address(orderData.getAddress())
                    .comments(orderData.getComments())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            String orderNumber = generateOrderNumber();
            order.setOrderNumber(orderNumber);

            Order savedOrder = orderRepository.save(order);

            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                OrderItem orderItem = OrderItem.builder()
                        .order(savedOrder)
                        .product(product)
                        .productName(product.getName())
                        .quantity(cartItem.getCountProduct())
                        .priceAtOrder(product.getAmount())
                        .build();

                OrderItem savedOrderItem = orderItemRepository.save(orderItem);

                savedOrder.getItems().add(savedOrderItem);

                if (cartItem.getAddons() != null && !cartItem.getAddons().isEmpty()) {
                    List<OrderItemAddon> orderItemAddons = new ArrayList<>();

                    for (CartItemAddon cartItemAddon : cartItem.getAddons()) {
                        Product addonProduct = cartItemAddon.getAddonProduct();

                        OrderItemAddon orderItemAddon = OrderItemAddon.builder()
                                .orderItem(savedOrderItem)
                                .addonProduct(addonProduct)
                                .quantity(cartItemAddon.getQuantity())
                                .priceAtOrder(cartItemAddon.getPriceAtSelection())
                                .addonProductName(addonProduct != null ? addonProduct.getName() : null)
                                .createdAt(LocalDateTime.now())
                                .build();

                        OrderItemAddon savedAddon = orderItemAddonRepository.save(orderItemAddon);
                        orderItemAddons.add(savedAddon);
                    }

                    savedOrderItem.setAddons(orderItemAddons);
                }

                product.setCount(product.getCount() - cartItem.getCountProduct());
                productService.saveProduct(product);
            }

            log.info("Заказ создан: ID={}, номер={}, пользователь={}, сумма={}",
                    savedOrder.getId(), savedOrder.getOrderNumber(),
                    user.getName(), savedOrder.getTotalAmount());

            // Отправляем заказ в кондитерский бот
            OrderRequest orderRequest = convertToOrderRequest(savedOrder);
            confectioneryBotClient.sendOrderToConfectionery(orderRequest);

            return savedOrder;

        } catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Конвертация сущности Order в OrderRequest для кондитерского бота
     */
    private OrderRequest convertToOrderRequest(Order order) {
        OrderRequest orderRequest = new OrderRequest();

        orderRequest.setOrderId(order.getId());
        orderRequest.setOrderNumber(order.getOrderNumber());
        orderRequest.setCustomerName(order.getUser().getName());
        orderRequest.setCustomerChatId(order.getUser().getChatId());
        orderRequest.setPhoneNumber(order.getPhoneNumber());
        orderRequest.setAddress(order.getAddress());
        orderRequest.setComments(order.getComments());
        orderRequest.setTotalAmount(order.getTotalAmount());
        orderRequest.setDeliveryType(order.getDeliveryType().getDescription());
        orderRequest.setStatus(order.getStatus().name());
        orderRequest.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<OrderRequest.OrderItemDto> itemDtos = new ArrayList<>();

            Map<String, List<OrderItem>> groupedItems = groupOrderItems(order.getItems());

            for (Map.Entry<String, List<OrderItem>> entry : groupedItems.entrySet()) {
                List<OrderItem> group = entry.getValue();
                OrderItem firstItem = group.getFirst();

                int totalQuantity = group.stream().mapToInt(OrderItem::getQuantity).sum();
                int pricePerItem = firstItem.getPriceAtOrder();
                int totalPrice = pricePerItem * totalQuantity;

                List<String> addons = new ArrayList<>();
                if (firstItem.getAddons() != null) {
                    addons = firstItem.getAddons().stream()
                            .map(addon -> addon.getAddonProductName() != null ?
                                    addon.getAddonProductName() : "Добавка")
                            .collect(Collectors.toList());
                }

                OrderRequest.OrderItemDto itemDto = new OrderRequest.OrderItemDto(
                        firstItem.getProductName(),
                        totalQuantity,
                        pricePerItem,
                        totalPrice,
                        addons
                );

                itemDtos.add(itemDto);
            }

            orderRequest.setItems(itemDtos);
        }

        return orderRequest;
    }

    /**
     * Группировка OrderItem по продукту и добавкам
     */
    private Map<String, List<OrderItem>> groupOrderItems(List<OrderItem> orderItems) {
        Map<String, List<OrderItem>> groupedMap = new LinkedHashMap<>();

        for (OrderItem item : orderItems) {
            String key = generateGroupKey(item);
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        return groupedMap;
    }

    /**
     * Генерация ключа для группировки
     */
    private String generateGroupKey(OrderItem orderItem) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(orderItem.getProduct().getId());

        if (orderItem.getAddons() != null && !orderItem.getAddons().isEmpty()) {
            String addonsKey = orderItem.getAddons().stream()
                    .map(addon -> addon.getAddonProduct() != null ?
                            String.valueOf(addon.getAddonProduct().getId()) : "")
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining(","));

            if (!addonsKey.isEmpty()) {
                keyBuilder.append("_").append(addonsKey);
            }
        }

        return keyBuilder.toString();
    }

    /**
     * Генерация номера заказа
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "ORD-" + timestamp.substring(timestamp.length() - 6) + "-" + random;
    }

    @Transactional
    public boolean updateOrderStatus(Long orderId, String status) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
                orderRepository.save(order);
                log.info("✅ Статус заказа {} обновлен на: {}", orderId, status);
                return true;
            } else {
                log.error("❌ Заказ с ID {} не найден", orderId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Ошибка обновления статуса заказа {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }
}
