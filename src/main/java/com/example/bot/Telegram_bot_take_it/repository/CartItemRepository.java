package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Cart;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Найти все товары в корзине
     */
    List<CartItem> findByCart(Cart cart);

    /**
     * Найти товар в корзине по ID корзины и ID продукта
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    List<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId,
                                                @Param("productId") Long productId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.product = :product")
    List<CartItem> findByCartAndProduct(@Param("cart") Cart cart, @Param("product") Product product);

    /**
     * Найти товар в корзине по корзине и продукту
     */
    //Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    /**
     * Посчитать количество товаров в корзине
     */
    Long countByCart(Cart cart);

    /**
     * Удалить все товары из корзины
     */
    void deleteByCart(Cart cart);

    // ДОБАВЬТЕ ЭТОТ МЕТОД
    @Query("SELECT ci FROM CartItem ci " +
            "LEFT JOIN FETCH ci.product " +
            "LEFT JOIN FETCH ci.addons a " +
            "LEFT JOIN FETCH a.addonProduct " +
            "WHERE ci.cart = :cart")
    List<CartItem> findByCartWithProductAndAddons(@Param("cart") Cart cart);

    /**
     * Найти все товары в корзине с загруженными добавками
     */
    @Query("SELECT DISTINCT ci FROM CartItem ci " +
            "LEFT JOIN FETCH ci.addons addons " +
            "LEFT JOIN FETCH addons.addonProduct " +
            "WHERE ci.cart.id = :cartId " +
            "ORDER BY ci.addedAt DESC")
    List<CartItem> findByCartIdWithAddons(@Param("cartId") Long cartId);

    /**
     * Удалить товар из корзины по ID корзины и ID продукта
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}