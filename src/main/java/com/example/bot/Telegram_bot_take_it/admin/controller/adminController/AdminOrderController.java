package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminOrderDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.OrderDto;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.admin.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления заказами в админ-панели.
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Позволяет:
 *  - получать список всех заказов
 *  - получать конкретный заказ по ID
 *  - изменять статус заказа
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    /**
     * Сервис с бизнес-логикой для работы с заказами в админской части
     */
    private final AdminOrderService adminOrderService;

    /**
     * Возвращает список всех заказов в системе.
     * <p>
     * Обычно используется для страницы управления заказами в админ-панели.
     *
     * @return список заказов в виде AdminOrderDto
     */
    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderDto>> getAllOrders() {
        List<Order> orders = adminOrderService.findAll();

        List<AdminOrderDto> result = orders.stream().map(o ->
                AdminOrderDto.builder()
                        .id(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus())
                        .deliveryType(o.getDeliveryType())
                        .userName(o.getUser() != null ? o.getUser().getName() : "Неизвестный")
                        .phoneNumber(o.getPhoneNumber())
                        .address(o.getAddress())
                        .comments(o.getComments())
                        .createdAt(o.getCreatedAt())
                        .build()
        ).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Обновляет статус заказа.
     * <p>
     * Используется, например, для перевода заказа в состояния:
     * "CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELLED" и т.п.
     *
     * @param id     идентификатор заказа
     * @param request новый статус заказа
     * @return обновлённый заказ
     */
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(adminOrderService.updateStatus(id, status));
    }

    /**
     * Возвращает список заказов, относящихся к пользователю с указанным username.
     * <p>
     * Используется для поиска заказов в админке по логину/username пользователя.
     *
     * @param username username пользователя, по которому нужно найти заказы
     * @return список заказов пользователя в виде AdminOrderDto
     */
    @GetMapping("/orders/search")
    public ResponseEntity<List<AdminOrderDto>> getOrdersByUsername(@RequestParam String username) {
        List<AdminOrderDto> orders = adminOrderService.getOrdersByUsername(username);
        return ResponseEntity.ok(orders);
    }

    /**
     * Возвращает подробную информацию о заказе по ID.
     * <p>
     * В отличие от списка (/orders), здесь возвращается OrderDto — обычно он содержит
     * расширенную информацию (например, товары/позиции заказа и т.п., в зависимости от реализации DTO).
     *
     * @param id идентификатор заказа
     * @return подробный заказ в виде OrderDto
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        OrderDto order = adminOrderService.getById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Обновляет заказ (в текущей реализации — только статус) по ID (второй вариант endpoint'а).
     * <p>
     * Ожидает тело запроса, где обязательно есть поле "status".
     * Если "status" не передан — возвращает 400 Bad Request.
     *
     * @param id      идентификатор заказа
     * @param updates JSON-объект с обновлениями (используется ключ "status")
     * @return заказ после обновления статуса в виде OrderDto
     */
    @PutMapping("/orders/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        String status = updates.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        OrderDto updatedOrder = adminOrderService.updateOrderStatusAdmin(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Удаляет позицию (товар / item) из заказа.
     * <p>
     * Используется, когда в админке нужно убрать конкретный item из конкретного заказа.
     * После успешного удаления возвращает 204 No Content.
     *
     * @param orderId идентификатор заказа
     * @param itemId  идентификатор позиции (item) внутри заказа
     * @return 204 No Content
     */
    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        adminOrderService.removeOrderItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
