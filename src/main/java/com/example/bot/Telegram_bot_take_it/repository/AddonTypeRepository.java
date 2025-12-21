package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.AddonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonTypeRepository extends JpaRepository<AddonType, Long> {

    /**
     * Найти все доступные товары в категории с количеством больше 0
     * Использует JPQL запрос для более сложной фильтрации
     */
    @Query("SELECT a FROM AddonType a WHERE a.isActive = true ORDER BY a.id")
    List<AddonType> findByCategoryIdAndAvailableTrueAndCountGreaterThanZero();
}
