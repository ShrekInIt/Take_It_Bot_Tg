package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.OrderDto;
import com.example.bot.Telegram_bot_take_it.admin.dto.AdminOrderDto;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderDto>> getAllOrders() {
        List<Order> orders = orderService.findAll();

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

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @GetMapping("/orders/search")
    public ResponseEntity<List<AdminOrderDto>> getOrdersByUsername(@RequestParam String username) {
        List<AdminOrderDto> orders = orderService.getOrdersByUsername(username);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        OrderDto order = orderService.getById(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/orders/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> updates) {

        String status = updates.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }

        OrderDto updatedOrder = orderService.updateOrderStatusAdmin(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        orderService.removeOrderItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}
