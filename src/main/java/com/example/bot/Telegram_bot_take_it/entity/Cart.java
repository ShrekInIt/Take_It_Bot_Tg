package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Рассчитать общую сумму корзины
     */
    public Long calculateTotalAmount() {
        return cartItems.stream()
                .mapToLong(CartItem::calculateItemTotal)
                .sum();
    }

    /**
     * Проверить, пуста ли корзина
     */
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    /**
     * Добавить товар в корзину
     */
    public void addItem(CartItem cartItem) {
        if (this.cartItems == null) {
            this.cartItems = new ArrayList<>();
        }
        cartItem.setCart(this);
        this.cartItems.add(cartItem);
    }
}
