package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.dto.response.OrderResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.mapper.OrderResponseMapper;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
import com.example.bot.Telegram_bot_take_it.utils.CartSnapshot;
import com.example.bot.Telegram_bot_take_it.utils.OrderCreatedEvent;
import com.example.bot.Telegram_bot_take_it.utils.interfaces.OrderUserNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationEventPublisher publisher;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemAddonRepository orderItemAddonRepository;
    private final UserService userService;
    private final CartService cartService;
    private final ProductService productService;
    private final OrderUserNotifier orderUserNotifier;
    private final OrderResponseMapper orderResponseMapper;

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
                loadAddonsForOrder(order);
            }
            return orders;
        } catch (Exception e) {
            log.error("Ошибка при получении заказов пользователя: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Long getChatIdByOrderId(Long orderId) {
        User user = orderRepository.findUserIdByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return user.getChatId();
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
    public void loadAddonsForOrder(Order order) {
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

            CartSnapshot snap = cartService.getCartSnapshot(chatId);
            List<CartItem> cartItems = getCartItems(snap);

            long totalAmount = snap.total();

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

                        if (addonProduct != null) {
                            int addonQuantityToDeduct = cartItemAddon.getQuantity() * cartItem.getCountProduct();
                            addonProduct.setCount(addonProduct.getCount() - addonQuantityToDeduct);
                            productService.saveProduct(addonProduct);
                        }
                    }
                    savedOrderItem.setAddons(orderItemAddons);
                }

                product.setCount(product.getCount() - cartItem.getCountProduct());
                productService.saveProduct(product);
            }

            log.info("Заказ создан: ID={}, номер={}, пользователь={}, сумма={}",
                    savedOrder.getId(), savedOrder.getOrderNumber(),
                    user.getName(), savedOrder.getTotalAmount());


            publisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));

            return savedOrder;

        }catch (IllegalArgumentException e) {
            log.warn("Невозможно создать заказ: {}", e.getMessage());
            throw e;
        }
        catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public OrderResponseDto createOrderFromCartDto(Long chatId, OrderData orderData) {
        return orderResponseMapper.toDto(createOrderFromCart(chatId, orderData));
    }

    @NotNull
    private static List<CartItem> getCartItems(CartSnapshot snap) {
        List<CartItem> cartItems = snap.items();

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

            if (cartItem.getAddons() != null && !cartItem.getAddons().isEmpty()) {
                for (CartItemAddon cartItemAddon : cartItem.getAddons()) {
                    Product addonProduct = cartItemAddon.getAddonProduct();
                    if (addonProduct != null) {
                        int requiredAddonQuantity = cartItemAddon.getQuantity() * cartItem.getCountProduct();
                        if (addonProduct.getCount() < requiredAddonQuantity) {
                            throw new IllegalArgumentException(
                                    String.format("Добавка '%s' недоступна в нужном количестве. Осталось: %d",
                                            addonProduct.getName(), addonProduct.getCount()));
                        }
                    }
                }
            }
        }
        return cartItems;
    }

    /**
     * Генерация номера заказа
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "ORD-" + timestamp.substring(timestamp.length() - 6) + "-" + random;
    }

    /**
     * Обновление статуса заказа
     */
    @Transactional
    public boolean updateOrderStatus(Long orderId, String status) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
                orderRepository.save(order);
                log.info("✅ Статус заказа {} обновлен на: {}", orderId, status);

                if (shouldNotifyUser(order.getStatus())) {
                    orderUserNotifier.sendStatusUpdateNotification(order);
                }

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

    /**
     * Проверяет необходимость уведомления пользователя о смене статуса заказа
     * и отправляет уведомление, если текущий статус предполагает оповещение.
     *
     * @param  newStatus статус которого был обновлён
     * @return true — если заказ найден и обработан,
     *         false — если заказ не найден или произошла ошибка
     */
    private boolean shouldNotifyUser(Order.OrderStatus newStatus) {
        return (newStatus == Order.OrderStatus.CONFIRMED ||
                newStatus == Order.OrderStatus.COMPLETED ||
                newStatus == Order.OrderStatus.READY ||
                newStatus == Order.OrderStatus.CANCELLED ||
                newStatus == Order.OrderStatus.DELIVERING ||
                newStatus == Order.OrderStatus.PENDING ||
                newStatus == Order.OrderStatus.PREPARING
        );
    }

    /**
     * Получить все заказы пользователя в DTO-формате.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getUserOrdersDto(Long chatId) {
        return getUserOrders(chatId).stream()
                .map(orderResponseMapper::toDto)
                .toList();
    }

    /**
     * Получить детали заказа по ID в DTO-формате.
     */
    @Transactional(readOnly = true)
    public Optional<OrderResponseDto> getOrderByIdAndUserDto(Long orderId, Long chatId) {
        return getOrderByIdAndUser(orderId, chatId)
                .map(orderResponseMapper::toDto);
    }

    /**
     * Скрыть все завершенные заказы пользователя.
     */
    @Transactional
    public int hideCompletedOrders(Long chatId) {
        try {
            User user = userService.getUserByChatId(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            List<Order> orders = orderRepository.findByUserWithItems(user);
            List<Order> completedOrders = orders.stream()
                    .filter(order -> order.getStatus() == Order.OrderStatus.COMPLETED)
                    .toList();

            for (Order order : completedOrders) {
                order.setVisible(false);
                orderRepository.save(order);
            }

            return completedOrders.size();

        } catch (Exception e) {
            log.error("Ошибка при скрытии завершенных заказов: {}", e.getMessage(), e);
            return 0;
        }
    }
}
