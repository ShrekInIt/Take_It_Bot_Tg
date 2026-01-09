package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        log.info("📨 Обновление статуса заказа {} на {}", orderId, status);

        try {
            boolean updated = orderService.updateOrderStatus(orderId, status);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Статус обновлен",
                        "orderId", orderId,
                        "status", status
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Заказ не найден"));
            }

        } catch (Exception e) {
            log.error("❌ Ошибка обновления статуса заказа {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
