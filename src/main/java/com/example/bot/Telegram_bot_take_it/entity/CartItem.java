package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "count_product")
    private Integer countProduct = 1;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemAddon> addons = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    /**
     * Рассчитать общую стоимость товара с учетом добавок
     */
    public Long calculateItemTotal() {
        long productTotal = (product.getAmount() != null ? product.getAmount() : 0) * countProduct;

        long addonsTotal = addons.stream()
                .mapToLong(addon -> (addon.getPriceAtSelection() != null ? addon.getPriceAtSelection() : 0) *
                        (addon.getQuantity() != null ? addon.getQuantity() : 0))
                .sum();

        return productTotal + addonsTotal;
    }

    /**
     * Рассчитать стоимость только товара (без добавок)
     */
    public Long calculateProductTotal() {
        return (product.getAmount() != null ? product.getAmount() : 0) * countProduct;
    }

    /**
     * Добавить добавку к товару
     */
    public void addAddon(CartItemAddon addon) {
        addon.setCartItem(this);
        addons.add(addon);
    }

    /**
     * Проверить, есть ли у товара добавки
     */
    public boolean hasAddons() {
        return !addons.isEmpty();
    }
}
