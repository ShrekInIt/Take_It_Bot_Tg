package com.example.bot.Telegram_bot_take_it.admin.service;

import com.example.bot.Telegram_bot_take_it.admin.dto.*;
import com.example.bot.Telegram_bot_take_it.admin.exception.ResourceNotFoundException;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderItemRepository;
import com.example.bot.Telegram_bot_take_it.repository.OrderRepository;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Сервис админ-панели для работы с заказами.
 * <p>
 * Содержит бизнес-логику:
 *  - получение заказов и статистики (активные/за сегодня/выручка)
 *  - получение последних заказов для dashboard
 *  - поиск заказов по username
 *  - получение детального заказа (items + addons)
 *  - удаление позиции заказа и пересчёт суммы
 *  - обновление статуса заказа с отправкой уведомлений в Telegram
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminOrderService {

    /** Репозиторий заказов */
    private final OrderRepository orderRepository;

    /** Репозиторий добавок к позициям заказа */
    private final OrderItemAddonRepository orderItemAddonRepository;

    /** Репозиторий позиций заказа */
    private final OrderItemRepository orderItemRepository;

    /** Маппер сущности Order -> OrderDto (админский DTO) */
    private final OrderMapper orderMapper;

    /** Сервис заказов (используется для вспомогательных операций: загрузка addons, chatId и т.п.) */
    private final OrderService orderService;

    /** Отправка сообщений пользователю в Telegram при изменении статуса */
    private final TelegramMessageSender telegramMessageSender;

    /**
     * Возвращает все заказы
     */
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    /**
     * Возвращает количество активных заказов
     */
    public long countActiveOrders() {
        return orderRepository.countByStatusIn(Order.OrderStatus.activeStatuses());
    }

    /**
     * Возвращает доход за сегодня
     */
    public Integer calculateTodayRevenue() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(1);
        Integer revenue = orderRepository.sumTotalAmountByDateOrderBetween(start, end);
        return revenue != null ? revenue : 0;
    }

    /**
     * Возвращает количество заказов за сегодня
     */
    public Integer countOrdersToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now();
        return orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    /**
     * Возвращает последние N заказов, user уже будет инициализирован (EntityGraph).
     */
    @Transactional(readOnly = true)
    public List<AdminOrderDto> getRecentOrders(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(order -> AdminOrderDto.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .deliveryType(order.getDeliveryType())
                        .userName(order.getUser() != null ? order.getUser().getName() : null)
                        .phoneNumber(order.getPhoneNumber())
                        .address(order.getAddress())
                        .comments(order.getComments())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Обновляет статус заказа по ID.
     *
     * @param id     ID заказа
     * @param status строковое имя enum-статуса (Order.OrderStatus.valueOf)
     * @return сохранённый Order после изменения статуса
     */
    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(Order.OrderStatus.valueOf(status));
        return orderRepository.save(order);
    }

    /**
     * Сохраняет заказ (обычный save в репозиторий).
     *
     * @param order заказ, который нужно сохранить
     * @return сохранённый заказ
     */
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Возвращает заказы пользователя по username.
     * <p>
     * username нормализуется: trim() и toLowerCase().
     *
     * @param username логин пользователя
     * @return список заказов пользователя в формате AdminOrderDto
     */
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

    /**
     * Возвращает детальную информацию о заказе по ID (с позициями и добавками).
     * <p>
     * Логика:
     *  - берёт OrderDto через orderRepository.findByIdWithDetails(...)
     *  - получает список OrderItem для заказа
     *  - мапит позиции в OrderItemDto (product id+name, qty, priceAtOrder)
     *  - для каждой позиции подгружает addons и кладёт в item.setAddons(...)
     *  - кладёт items в order.setItems(items)
     *
     * @param id ID заказа
     * @return подробный OrderDto
     */
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

    /**
     * Удаляет позицию (OrderItem) из заказа и пересчитывает итоговую сумму.
     * <p>
     * Логика:
     *  - проверяет, что заказ существует
     *  - проверяет, что itemId реально есть в заказе
     *  - удаляет item из коллекции order.getItems() и из БД через orderItemRepository.delete
     *  - подгружает addons в заказ (orderService.loadAddonsForOrder)
     *  - пересчитывает totalAmount: (priceAtOrder * qty) + сумма(addon.priceAtOrder * addon.qty) по всем items
     *  - сохраняет заказ
     *
     * @param orderId ID заказа
     * @param itemId  ID позиции заказа
     */
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

        orderService.loadAddonsForOrder(order);

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
     * Обновляет статус заказа администратором и возвращает обновлённый DTO.
     * <p>
     * Дополнительно:
     *  - валидирует, что statusStr не пустой
     *  - приводит статус к верхнему регистру и делает valueOf
     *  - если переводим заказ в "Подтвержден" или "Завершен" (по description)
     *    и статус реально меняется — отправляет пользователю уведомление в Telegram
     *
     * @param orderId   ID заказа
     * @param statusStr новый статус (строкой)
     * @return OrderDto после обновления статуса
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
                Long chatId = orderService.getChatIdByOrderId(orderId);
                telegramMessageSender.sendMessageHtml(chatId, "*Ваш заказ подтверждён!*\n\nНаши кондитеры уже начали готовить ваш заказ. Обычно приготовление занимает 20-30 минут.");
            }
            if(Objects.equals(statusEnum.getDescription(), "Завершен") && !order.getStatus().equals(statusEnum)){
                Long chatId = orderService.getChatIdByOrderId(orderId);
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
