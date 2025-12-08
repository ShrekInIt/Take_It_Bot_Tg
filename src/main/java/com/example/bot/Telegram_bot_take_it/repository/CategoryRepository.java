package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Получить все активные корневые категории (без родителя)
     * Сортировка по полю sortOrder
     */
    List<Category> findByParentIdIsNullAndIsActiveTrueOrderBySortOrder();

    /**
     * Получить активные подкатегории для указанной родительской категории
     * Сортировка по полю sortOrder
     */
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrder(Long parentId);
}
