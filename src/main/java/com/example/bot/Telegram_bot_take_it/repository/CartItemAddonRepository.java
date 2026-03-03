package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemAddonRepository extends JpaRepository<CartItemAddon, Long> {

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
     * Удалить добавку по ID товара в корзине и ID продукта добавки
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId AND cia.addonProduct.id = :addonProductId")
    void deleteByCartItemIdAndAddonProductId(@Param("cartItemId") Long cartItemId,
                                             @Param("addonProductId") Long addonProductId);

    /**
     * Проверить, есть ли добавки у cart_item
     */
    @Query("SELECT CASE WHEN COUNT(cia) > 0 THEN true ELSE false END FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    Boolean existsByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Найти все продукты-добавки определенной категории для cart_item
     */
    @Query("SELECT p FROM CartItemAddon cia " +
            "JOIN cia.addonProduct p " +
            "JOIN FETCH p.category c " +
            "WHERE cia.cartItem.id = :cartItemId AND c.id = :categoryId")
    List<Product> findAddonProductsByCartItemIdAndCategoryId(@Param("cartItemId") Long cartItemId,
                                                             @Param("categoryId") Long categoryId);
}
