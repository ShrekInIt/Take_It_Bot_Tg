package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.admin.dto.*;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.dto.AdminOrderDto;
import com.example.bot.Telegram_bot_take_it.dto.OrderData;
import com.example.bot.Telegram_bot_take_it.dto.OrderRequest;
import com.example.bot.Telegram_bot_take_it.entity.*;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
import com.example.bot.Telegram_bot_take_it.utils.ConfectioneryBotClient;
import com.example.bot.Telegram_bot_take_it.utils.OrderStatusNotifier;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    private final OrderStatusNotifier orderStatusNotifier;
    private final OrderMapper orderMapper;
    private final TelegramMessageSender telegramMessageSender;

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
                            .collect(toList());
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
                    orderStatusNotifier.sendStatusUpdateNotification(order);
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
                newStatus == Order.OrderStatus.COMPLETED);
    }

    public long countActiveOrders() {
        return orderRepository.countByStatusIn(List.of());
    }

    public long getTodayRevenue() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        Long revenue = orderRepository.sumTotalAmountByDateAndStatus(
                startOfDay, endOfDay, "completed");
        return revenue != null ? revenue : 0;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public List<Order> findRecentOrders() {
        return orderRepository.findTop10ByOrderByDateOrderDesc();
    }

    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(Order.OrderStatus.valueOf(status));
        return orderRepository.save(order);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public List<AdminOrderDto> getOrdersByUsername(String username) {
        List<Order> orders = orderRepository.findByUsername(username.trim().toLowerCase());

        return orders.stream().map(o -> AdminOrderDto.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .deliveryType(o.getDeliveryType())
                .userName(o.getUser().getName())
                .phoneNumber(o.getPhoneNumber())
                .address(o.getAddress())
                .comments(o.getComments())
                .createdAt(o.getCreatedAt())
                .build()
        ).collect(toList());

    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {

        OrderDto order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        List<OrderItem> orderItems = orderItemRepository.findItemsByOrderId(id);

        List<OrderItemDto> items = orderItems.stream()
                .map(i -> new OrderItemDto(
                        i.getId(),
                        new ProductDto(i.getProduct().getId(), i.getProduct().getName()),
                        i.getQuantity(),
                        i.getPriceAtOrder()
                ))
                .toList();

        items.forEach(item -> {
            List<OrderItemAddonDto> addons =
                    orderItemAddonRepository.findByOrderItemId(item.getId());
            item.setAddons(addons);
        });

        order.setItems(items);

        return order;
    }

    @Transactional
    public void removeOrderItem(Long orderId, Long itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Optional<OrderItem> maybeItem = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst();

        if (maybeItem.isEmpty()) {
            throw new ResourceNotFoundException("Order item not found: " + itemId);
        }

        OrderItem item = maybeItem.get();
        order.getItems().remove(item);
        orderItemRepository.delete(item);

        loadAddonsForOrder(order);

        int total = order.getItems().stream()
                .mapToInt(i -> {
                    int itemSum = (i.getPriceAtOrder() == null ? 0 : i.getPriceAtOrder()) * (i.getQuantity() == null ? 1 : i.getQuantity());
                    int addonsSum = 0;
                    if (i.getAddons() != null) {
                        addonsSum = i.getAddons().stream()
                                .mapToInt(a -> (a.getPriceAtOrder() == null ? 0 : a.getPriceAtOrder()) * (a.getQuantity() == null ? 1 : a.getQuantity()))
                                .sum();
                    }
                    return itemSum + addonsSum;
                })
                .sum();
        order.setTotalAmount(total);

        orderRepository.save(order);
    }

    /**
     * Обновляет поля заказа (в этом примере — статус) и возвращает обновлённый DTO.
     * Валидация статуса должна соответствовать enum'у в Order (Order.OrderStatus).
     */
    @Transactional
    public OrderDto updateOrderStatusAdmin(Long orderId, String statusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        try {
            Order.OrderStatus statusEnum = Order.OrderStatus.valueOf(statusStr.toUpperCase());
            if(Objects.equals(statusEnum.getDescription(), "Подтвержден") && !order.getStatus().equals(statusEnum)) {
                Long chatId = getChatIdByOrderId(orderId);
                telegramMessageSender.sendMessageHtml(chatId, "*Ваш заказ подтверждён!*\n\nНаши кондитеры уже начали готовить ваш заказ. Обычно приготовление занимает 20-30 минут.");
            }
            if(Objects.equals(statusEnum.getDescription(), "Завершен") && !order.getStatus().equals(statusEnum)){
                Long chatId = getChatIdByOrderId(orderId);
                telegramMessageSender.sendMessageHtml(chatId, "*Заказ завершён!*\n\nСпасибо за заказ! Надеемся, вам понравилось! 😊");
            }
            order.setStatus(statusEnum);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown status: " + statusStr);
        }

        orderRepository.save(order);
        return orderMapper.toDto(order);
    }
}
