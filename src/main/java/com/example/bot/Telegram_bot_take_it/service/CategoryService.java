package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Получить категорию по ID с загрузкой необходимых полей
     * @Transactional гарантирует, что сессия будет открыта
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId).orElse(null);
    }

    /**
     * Получить категорию с загрузкой parent (для безопасного доступа)
     */
    @Transactional(readOnly = true)
    public Category getCategoryWithParent(Long categoryId) {
        return categoryRepository.findByIdWithParent(categoryId).orElse(null);
    }

    /**
     * Получить текстовое описание для категории
     */
    public String getCategoryDescription(Category category, boolean hasSubcategories, boolean hasProducts) {
        if (category == null) return "❌ Категория не найдена";

        if (hasSubcategories && hasProducts) {
            return "☕ *" + category.getName() + "*\n\nВыберите подкатегорию или товар:";
        } else if (hasSubcategories) {
            return "☕ *" + category.getName() + "*\n\nВыберите подкатегорию:";
        } else if (hasProducts) {
            return "*" + category.getName() + "*\n\nВыберите товар:";
        } else {
            return "📭 В этой категории пока нет товаров";
        }
    }

    /**
     * Получить активные подкатегории
     */
    @Transactional(readOnly = true)
    public List<Category> getActiveSubcategories(Long parentId) {
        List<Category> subs = categoryRepository.findByParentIdAndActiveTrueOrderBySortOrder(parentId);
        subs.removeIf(cat -> "Добавки".equals(cat.getName()));
        return subs;
    }

    /**
     * Проверить, является ли категорией кофе (по id категории)
     */
    @Transactional(readOnly = true)
    public boolean isCoffeeCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(category -> category.getCategoryType() != null
                        && "COFFEE".equalsIgnoreCase(category.getCategoryType().getCode()))
                .orElse(false);
    }

    /**
     * Получить активные корневые категории
     */
    @Transactional(readOnly = true)
    public List<Category> getActiveRootCategories() {
        return categoryRepository.findByParentIdIsNullAndActiveTrueOrderBySortOrder();
    }
}
