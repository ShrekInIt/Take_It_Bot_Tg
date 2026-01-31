package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /**
     * Найти все доступные товары в категории с количеством больше 0
     * Использует JPQL запрос для более сложной фильтрации
     */
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId AND p.available = true AND p.count > 0 ORDER BY p.id")
    List<Product> findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(@Param("categoryId") Long categoryId);

    long countByAvailable(boolean available);

    @NotNull
    @Query("SELECT p FROM Product p JOIN FETCH p.category order by p.id")
    List<Product> findAll();

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE lower(p.name) LIKE lower(concat('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.category = :newCategory WHERE p.category.id = :oldCategoryId")
    void moveProductsToAnotherCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategory") Category newCategory);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

}
