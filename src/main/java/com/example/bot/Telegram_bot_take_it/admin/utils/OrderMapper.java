package com.example.bot.Telegram_bot_take_it.admin.utils;

import com.example.bot.Telegram_bot_take_it.admin.dto.*;
import com.example.bot.Telegram_bot_take_it.dto.OrderItemDtoBot;
import com.example.bot.Telegram_bot_take_it.dto.OrderRequest;
import com.example.bot.Telegram_bot_take_it.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Маппер (конвертер) объектов между сущностями (Entity) и DTO.
 * <p>
 * Этот класс используется в админке и в части интеграции с "кондитерским ботом":
 *  - Order -> OrderDto (подробный заказ для админ-панели)
 *  - Category/Product/User -> Admin*Dto (формат для таблиц админки)
 *  - Order -> OrderRequest (формат заказа для внешнего/другого бота)
 * <p>
 * Также содержит утилиты для группировки позиций заказа по товару и набору добавок.
 */
@Component
public class OrderMapper {

    /**
     * Конвертирует сущность Order в DTO для админки (OrderDto).
     * <p>
     * Заполняет основные поля заказа:
     *  - id, createdAt, статус, сумма
     *  - адрес доставки, комментарий
     *  - пользователя (через toUserDto)
     *  - список позиций (items) (через toItemDto)
     * <p>
     * Если order == null -> возвращает null.
     * Если items == null -> ставит пустой список.
     *
     * @param order сущность заказа из БД
     * @return OrderDto для отдачи на фронт админки
     */
    public OrderDto toDto(Order order) {
        if (order == null) return null;

        OrderDto dto = new OrderDto();

        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStatus(order.getStatus().name().toUpperCase());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDeliveryAddress(order.getAddress());
        dto.setComment(order.getComments());

        dto.setUser(toUserDto(order.getUser()));

        if (order.getItems() != null) {
            dto.setItems(
                    order.getItems()
                            .stream()
                            .map(this::toItemDto)
                            .toList()
            );
        } else {
            dto.setItems(List.of());
        }

        return dto;
    }

    /**
     * Конвертирует сущность User в UserDto (упрощённый пользователь для заказа).
     * <p>
     * Заполняет:
     *  - id
     *  - username (берётся из user.getName())
     *  - telegramId
     *  - phoneNumber
     * <p>
     * Если user == null -> возвращает null.
     *
     * @param user сущность пользователя
     * @return UserDto для вложения в OrderDto
     */
    private UserDto toUserDto(User user) {
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setId(user.getId());

        dto.setUsername(user.getName());

        dto.setTelegramId(user.getTelegramId());
        dto.setPhoneNumber(user.getPhoneNumber());

        return dto;
    }

    /**
     * Конвертирует сущность OrderItem (позиция заказа) в OrderItemDto.
     * <p>
     * Заполняет:
     *  - id, quantity
     *  - price (берётся из priceAtOrder)
     *  - product.name (берётся из item.getProductName())
     *  - addons (если есть) через toAddonDto
     * <p>
     * Важно: здесь ProductDto создаётся и заполняется только name,
     * ID товара не заполняется (по логике текущего файла).
     * <p>
     * Если addons == null -> ставит пустой список.
     *
     * @param item позиция заказа
     * @return OrderItemDto для отдачи на фронт админки
     */
    private OrderItemDto toItemDto(OrderItem item) {
        if (item == null) return null;

        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPriceAtOrder());

        ProductDto productDto = new ProductDto();
        productDto.setName(item.getProductName());
        dto.setProduct(productDto);

        if (item.getAddons() != null) {
            dto.setAddons(
                    item.getAddons()
                            .stream()
                            .map(this::toAddonDto)
                            .toList()
            );
        } else {
            dto.setAddons(List.of());
        }

        return dto;
    }

    /**
     * Конвертирует сущность OrderItemAddon (добавка к позиции) в OrderItemAddonDto.
     * <p>
     * Заполняет:
     *  - id
     *  - name (берётся из addonProductName)
     *  - quantity
     *  - price (берётся из priceAtOrder)
     *
     * @param addon сущность добавки
     * @return DTO добавки
     */
    private OrderItemAddonDto toAddonDto(OrderItemAddon addon) {
        if (addon == null) return null;

        OrderItemAddonDto dto = new OrderItemAddonDto();
        dto.setId(addon.getId());
        dto.setName(addon.getAddonProductName());
        dto.setQuantity(addon.getQuantity());
        dto.setPrice(addon.getPriceAtOrder());

        return dto;
    }

    /**
     * Конвертирует Category в AdminCategoryDto для админки.
     * <p>
     * Дополнительно вытягивает данные связей:
     *  - parentId / parentName (если есть родитель)
     *  - categoryTypeId / categoryTypeName (если задан тип категории)
     *
     * @param c сущность категории
     * @return AdminCategoryDto для отображения в админ-панели
     */
    public static AdminCategoryDto toDtoCategory(Category c) {
        return AdminCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .categoryTypeId(c.getCategoryType() != null ? c.getCategoryType().getId() : null)
                .categoryTypeName(c.getCategoryType() != null ? c.getCategoryType().getName() : null)
                .build();
    }

    /**
     * Конвертирует Product в AdminProductDto для админки.
     * <p>
     * Дополнительно:
     *  - вытягивает categoryId/categoryName, если категория задана
     *
     * @param p сущность товара
     * @return AdminProductDto для админ-панели
     */
    public static AdminProductDto toDtoProduct(Product p) {
        return AdminProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .amount(p.getAmount())
                .available(p.getAvailable())
                .description(p.getDescription())
                .photo(p.getPhoto())
                .size(p.getSize())
                .count(p.getCount())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }

    /**
     * Конвертирует User в AdminUserDto для админки.
     * <p>
     * Используется для таблицы пользователей/деталей пользователя.
     *
     * @param c сущность пользователя
     * @return AdminUserDto
     */
    public static AdminUserDto toDtoUser(User c) {
        return AdminUserDto.builder()
                .id(c.getId())
                .name(c.getName())
                .isActive(c.getIsActive())
                .telegramId(c.getTelegramId())
                .isAdmin(c.getIsAdmin())
                .chatId(c.getChatId())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt())
                .build();
    }

    /**
     * Конвертация сущности Order в OrderRequest для кондитерского бота
     */
    public static OrderRequest convertToOrderRequest(Order order) {
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
            List<OrderItemDtoBot> itemDtos = new ArrayList<>();

            Map<String, List<OrderItem>> groupedItems = groupOrderItems(order.getItems());

            for (Map.Entry<String, List<OrderItem>> entry : groupedItems.entrySet()) {
                List<OrderItem> group = entry.getValue();
                OrderItem firstItem = group.getFirst();

                int totalQuantity = group.stream().mapToInt(OrderItem::getQuantity).sum();
                long pricePerItem = firstItem.getPriceAtOrder() != null ? firstItem.getPriceAtOrder() : 0L;
                long totalPrice = pricePerItem * totalQuantity;

                List<String> addons = new ArrayList<>();
                if (firstItem.getAddons() != null) {
                    addons = firstItem.getAddons().stream()
                            .map(addon -> addon.getAddonProductName() != null ?
                                    addon.getAddonProductName() : "Добавка")
                            .collect(toList());
                }

                OrderItemDtoBot itemDto = new OrderItemDtoBot(
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
    private static Map<String, List<OrderItem>> groupOrderItems(List<OrderItem> orderItems) {
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
    private static String generateGroupKey(OrderItem orderItem) {
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
}
