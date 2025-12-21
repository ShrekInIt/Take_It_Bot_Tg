package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_item_addon",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_item_id", "addon_product_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_product_id", nullable = false)
    private Product addonProduct;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "price_at_selection", nullable = false)
    private Integer priceAtSelection;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Рассчитать стоимость добавки
     */
    public Integer calculateAddonTotal() {
        return priceAtSelection * (quantity != null ? quantity : 1);
    }
}
