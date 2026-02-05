package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import org.jetbrains.annotations.NotNull;
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

    /**
     * Получить активные подкатегории для указанной родительской категории
     * Сортировка по полю id
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.id = :id")
    Optional<Category> findByIdWithParent(@Param("id") Long id);

    @NotNull
    @Query("SELECT c FROM Category c ORDER BY c.id")
    List<Category> findAll();

    @Query("""
    select c from Category c
    left join fetch c.parent
    left join fetch c.categoryType
    where lower(c.name) like lower(concat('%', :name, '%'))
""")
    List<Category> findByNameContainingIgnoreCase(@Param("name") String name);

    Optional<Category> findByNameIgnoreCase(String name);

    @Query("""
    SELECT c FROM Category c
    LEFT JOIN FETCH c.parent
    LEFT JOIN FETCH c.categoryType
    """)
    List<Category> findAllWithRelations();

    @Query("""
    SELECT c FROM Category c
    LEFT JOIN FETCH c.parent
    LEFT JOIN FETCH c.categoryType
    WHERE c.id = :id
""")
    Optional<Category> findByIdWithRelations(@Param("id") Long id);

}
