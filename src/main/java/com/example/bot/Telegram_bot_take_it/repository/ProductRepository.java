package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /**
     * Найти все доступные товары в категории с количеством больше 0
     * Использует JPQL запрос для более сложной фильтрации
     */
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.available = true AND p.count > 0 ORDER BY p.id")
    List<Product> findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(@Param("categoryId") Long categoryId);

}
