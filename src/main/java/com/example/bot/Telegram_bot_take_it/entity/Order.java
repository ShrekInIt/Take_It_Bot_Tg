package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "comments")
    private String comments;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "visible", nullable = false)
    @Builder.Default
    private Boolean visible = true;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }

        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%05d", (int)(Math.random() * 100000));
        return "ORD-" + datePart + "-" + randomPart;
    }

    @Getter
    public enum OrderStatus {
        PENDING("Ожидает подтверждения"),
        CONFIRMED("Подтвержден"),
        PREPARING("Готовится"),
        READY("Готов к выдаче"),
        DELIVERING("Доставляется"),
        COMPLETED("Завершен"),
        CANCELLED("Отменен");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }
    }

    @Getter
    public enum DeliveryType {
        PICKUP("Самовывоз"),
        DELIVERY("Доставка");

        private final String description;

        DeliveryType(String description) {
            this.description = description;
        }
    }
}
