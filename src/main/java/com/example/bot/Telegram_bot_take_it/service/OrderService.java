package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
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

            return savedOrder;

        } catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Генерация номера заказа
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "ORD-" + timestamp.substring(timestamp.length() - 6) + "-" + random;
    }
}
