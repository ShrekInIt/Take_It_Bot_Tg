package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_addon")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemAddon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_product_id")
    private Product addonProduct;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price_at_order", nullable = false)
    private Integer priceAtOrder;

    @Column(name = "addon_product_name")
    private String addonProductName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
