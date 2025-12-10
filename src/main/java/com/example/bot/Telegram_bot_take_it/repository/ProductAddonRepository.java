package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.ProductAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAddonRepository extends JpaRepository<ProductAddon, Long> {
    /**
     * Найти все добавки для продукта
     */
    List<ProductAddon> findByProductId(Long productId);

    /**
     * Найти добавки для продукта по категории добавки
     */
    @Query("SELECT pa FROM ProductAddon pa WHERE pa.product.id = :productId AND pa.addonProduct.categoryId = :categoryId")
    List<ProductAddon> findByProductIdAndAddonProduct_CategoryId(@Param("productId") Long productId,
                                                                 @Param("categoryId") Long categoryId);

    /**
     * Найти конкретную добавку для продукта
     */
    Optional<ProductAddon> findByProductIdAndAddonProductId(Long productId, Long addonProductId);
}
