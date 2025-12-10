package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemAddonRepository extends JpaRepository<CartItemAddon, Long> {

    /**
     * Найти все добавки для товара в корзине
     */
    List<CartItemAddon> findByCartItem(CartItem cartItem);

    /**
     * Найти добавку по товару в корзине и продукту добавки
     */
    @Query("SELECT cia FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId AND cia.addonProduct.id = :addonProductId")
    Optional<CartItemAddon> findByCartItemIdAndAddonProductId(@Param("cartItemId") Long cartItemId,
                                                              @Param("addonProductId") Long addonProductId);

    /**
     * Найти добавку по товару в корзине и продукту добавки
     */
    Optional<CartItemAddon> findByCartItemAndAddonProduct(CartItem cartItem, Product addonProduct);

    /**
     * Удалить все добавки для товара в корзине
     */
    void deleteByCartItem(CartItem cartItem);

    /**
     * Удалить добавку по ID товара в корзине и ID продукта добавки
     */
    @Query("DELETE FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId AND cia.addonProduct.id = :addonProductId")
    void deleteByCartItemIdAndAddonProductId(@Param("cartItemId") Long cartItemId,
                                             @Param("addonProductId") Long addonProductId);

    /**
     * Посчитать количество добавок для товара в корзине
     */
    Long countByCartItem(CartItem cartItem);
}
