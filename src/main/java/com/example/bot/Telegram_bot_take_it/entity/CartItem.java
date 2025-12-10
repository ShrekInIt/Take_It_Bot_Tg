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
    public Integer calculateItemTotal() {
        // Стоимость основного товара
        int productTotal = (product.getAmount() != null ? product.getAmount() : 0) * countProduct;

        // Стоимость добавок
        int addonsTotal = addons.stream()
                .mapToInt(addon -> (addon.getPriceAtSelection() != null ? addon.getPriceAtSelection() : 0) *
                        (addon.getQuantity() != null ? addon.getQuantity() : 0))
                .sum();

        return productTotal + addonsTotal;
    }

    /**
     * Рассчитать стоимость только товара (без добавок)
     */
    public Integer calculateProductTotal() {
        return (product.getAmount() != null ? product.getAmount() : 0) * countProduct;
    }

    /**
     * Рассчитать стоимость только добавок
     */
    public Integer calculateAddonsTotal() {
        return addons.stream()
                .mapToInt(addon -> (addon.getPriceAtSelection() != null ? addon.getPriceAtSelection() : 0) *
                        (addon.getQuantity() != null ? addon.getQuantity() : 0))
                .sum();
    }

    /**
     * Получить общее количество добавок
     */
    public Integer getTotalAddonsCount() {
        return addons.stream()
                .mapToInt(CartItemAddon::getQuantity)
                .sum();
    }

    /**
     * Добавить добавку к товару
     */
    public void addAddon(CartItemAddon addon) {
        addon.setCartItem(this);
        addons.add(addon);
    }

    /**
     * Удалить добавку из товара
     */
    public void removeAddon(CartItemAddon addon) {
        addons.remove(addon);
        addon.setCartItem(null);
    }

    /**
     * Найти добавку по ID продукта
     */
    public CartItemAddon findAddonByProductId(Long addonProductId) {
        return addons.stream()
                .filter(addon -> addon.getAddonProduct().getId().equals(addonProductId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Проверить, есть ли у товара добавки
     */
    public boolean hasAddons() {
        return !addons.isEmpty();
    }

    /**
     * Получить описание товара с добавками
     */
    public String getDescriptionWithAddons() {
        StringBuilder description = new StringBuilder();
        description.append(product.getName())
                .append(" x").append(countProduct);

        if (hasAddons()) {
            description.append("\nДобавки:");
            for (CartItemAddon addon : addons) {
                description.append("\n  - ")
                        .append(addon.getAddonProduct().getName())
                        .append(" x").append(addon.getQuantity())
                        .append(" (+").append(addon.getPriceAtSelection()).append("₽ каждый)");
            }
        }

        if (specialInstructions != null && !specialInstructions.isEmpty()) {
            description.append("\nКомментарий: ").append(specialInstructions);
        }

        return description.toString();
    }
}
