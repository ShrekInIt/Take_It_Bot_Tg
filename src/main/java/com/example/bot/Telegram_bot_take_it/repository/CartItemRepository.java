package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Cart;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Найти товар в корзине по ID корзины и ID продукта
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    List<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId,
                                                @Param("productId") Long productId);

    /**
     * Найти товары по товарам
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.product = :product")
    List<CartItem> findByCartAndProduct(@Param("cart") Cart cart, @Param("product") Product product);

    /**
     * Удалить все товары из корзины
     */
    void deleteByCart(Cart cart);

    @Query("SELECT ci FROM CartItem ci " +
            "LEFT JOIN FETCH ci.product " +
            "LEFT JOIN FETCH ci.addons a " +
            "LEFT JOIN FETCH a.addonProduct " +
            "WHERE ci.cart = :cart")
    List<CartItem> findByCartWithProductAndAddons(@Param("cart") Cart cart);

    /**
     * Найти товары по имени и корзине
     */
    @Query("SELECT ci FROM CartItem ci " +
            "JOIN ci.product p " +
            "WHERE ci.cart.id = :cartId " +
            "AND p.name = :productName")
    List<CartItem> findByCartIdAndProductName(
            @Param("cartId") Long cartId,
            @Param("productName") String productName
    );
}