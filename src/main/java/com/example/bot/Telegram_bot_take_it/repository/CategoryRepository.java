package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Получить все активные корневые категории (без родителя)
     * Сортировка по полю sortOrder
     */
    List<Category> findByParentIdIsNullAndActiveTrueOrderBySortOrder();

    /**
     * Получить активные подкатегории для указанной родительской категории
     * Сортировка по полю sortOrder
     */
    List<Category> findByParentIdAndActiveTrueOrderBySortOrder(Long parentId);

    // Добавьте этот метод
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.id = :id")
    Optional<Category> findByIdWithParent(@Param("id") Long id);
}
