package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemAddonRepository extends JpaRepository<CartItemAddon, Long> {

    /**
     * Найти все добавки для товара в корзине по объекту CartItem
     */
    List<CartItemAddon> findByCartItem(CartItem cartItem);

    /**
     * Найти все добавки для товара в корзине по ID cart_item
     */
    @Query("SELECT cia FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    List<CartItemAddon> findByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Найти все добавки для товара в корзине по ID cart_item с продуктами
     */
    @Query("SELECT cia FROM CartItemAddon cia JOIN FETCH cia.addonProduct WHERE cia.cartItem.id = :cartItemId")
    List<CartItemAddon> findByCartItemIdWithProducts(@Param("cartItemId") Long cartItemId);

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
     * Найти все ID добавок для конкретного cart_item
     */
    @Query("SELECT cia.addonProduct.id FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    List<Long> findAddonProductIdsByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Найти все продукты-добавки для конкретного cart_item
     */
    @Query("SELECT cia.addonProduct FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    List<Product> findAddonProductsByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Посчитать количество добавок для cart_item
     */
    @Query("SELECT COUNT(cia) FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    Integer countByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Посчитать общую стоимость добавок для cart_item
     */
    @Query("SELECT COALESCE(SUM(cia.priceAtSelection * cia.quantity), 0) FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    Integer calculateTotalAddonsPriceForCartItem(@Param("cartItemId") Long cartItemId);

    /**
     * Удалить все добавки для товара в корзине
     */
    void deleteByCartItem(CartItem cartItem);

    /**
     * Удалить все добавки по ID товара в корзине
     */
    @Modifying
    @Query("DELETE FROM CartItemAddon cia WHERE cia.cartItem.id = :cartItemId")
    void deleteByCartItemId(@Param("cartItemId") Long cartItemId);

    /**
     * Удалить добавку по ID товара в корзине и ID продукта добавки
     */
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
     * Найти добавку определенной категории для cart_item
     */
    @Query("SELECT cia FROM CartItemAddon cia " +
            "JOIN cia.addonProduct p " +
            "JOIN p.category c " +
            "WHERE cia.cartItem.id = :cartItemId AND c.id = :categoryId")
    List<CartItemAddon> findByCartItemIdAndCategoryId(@Param("cartItemId") Long cartItemId,
                                                      @Param("categoryId") Long categoryId);

    /**
     * Найти первую добавку определенной категории для cart_item
     */
    @Query("SELECT cia FROM CartItemAddon cia " +
            "JOIN cia.addonProduct p " +
            "JOIN p.category c " +
            "WHERE cia.cartItem.id = :cartItemId AND c.id = :categoryId " +
            "ORDER BY cia.id ASC")
    Optional<CartItemAddon> findFirstByCartItemIdAndCategoryId(@Param("cartItemId") Long cartItemId,
                                                               @Param("categoryId") Long categoryId);

    /**
     * Найти все продукты-добавки определенной категории для cart_item
     */
    @Query("SELECT cia.addonProduct FROM CartItemAddon cia " +
            "JOIN cia.addonProduct p " +
            "JOIN p.category c " +
            "WHERE cia.cartItem.id = :cartItemId AND c.id = :categoryId")
    List<Product> findAddonProductsByCartItemIdAndCategoryId(@Param("cartItemId") Long cartItemId,
                                                             @Param("categoryId") Long categoryId);
}
