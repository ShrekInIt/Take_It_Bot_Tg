package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.response.CategoryResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.CategoryType;
import com.example.bot.Telegram_bot_take_it.mapper.CategoryMapper;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import com.example.bot.Telegram_bot_take_it.repository.CategoryTypeRepository;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final CategoryMapper categoryMapper;

    // ==================== DTO МЕТОДЫ (основные для handlers) ====================

    /**
     * Получить категорию с родителем в виде DTO
     */
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryWithParentDto(Long categoryId) {
        Category category = categoryRepository.findByIdWithParent(categoryId).orElse(null);
        if (category == null) return null;
        boolean hasSubcategories = !getActiveSubcategoriesInternal(categoryId).isEmpty();
        return categoryMapper.toResponseDto(category, hasSubcategories);
    }

    /**
     * Получить активные подкатегории в виде DTO с информацией о наличии подкатегорий
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getActiveSubcategoriesDto(Long parentId) {
        return getActiveSubcategoriesInternal(parentId).stream()
                .map(category -> {
                    boolean hasSubcategories = !getActiveSubcategoriesInternal(category.getId()).isEmpty();
                    return categoryMapper.toResponseDto(category, hasSubcategories);
                })
                .toList();
    }

    /**
     * Получить активные корневые категории в виде DTO с информацией о наличии подкатегорий
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getActiveRootCategoriesDto() {
        return getActiveRootCategoriesInternal().stream()
                .map(category -> {
                    boolean hasSubcategories = !getActiveSubcategoriesInternal(category.getId()).isEmpty();
                    return categoryMapper.toResponseDto(category, hasSubcategories);
                })
                .toList();
    }

    /**
     * Получить описание категории для отображения
     */
    public String getCategoryDescription(CategoryResponseDto category, boolean hasSubcategories, boolean hasProducts) {
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
     * Проверить, является ли категорией кофе (по id категории)
     */
    @Transactional(readOnly = true)
    public boolean isCoffeeCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(category -> category.getCategoryType() != null
                        && "COFFEE".equalsIgnoreCase(category.getCategoryType().getCode()))
                .orElse(false);
    }

    // ==================== ВНУТРЕННИЕ МЕТОДЫ (private) ====================

    /**
     * Внутренний метод для получения подкатегорий (возвращает entity)
     */
    private List<Category> getActiveSubcategoriesInternal(Long parentId) {
        List<Category> subs = categoryRepository.findByParentIdAndActiveTrueOrderBySortOrder(parentId);
        subs.removeIf(cat -> "Добавки".equals(cat.getName()));
        return subs;
    }

    /**
     * Внутренний метод для получения корневых категорий (возвращает entity)
     */
    private List<Category> getActiveRootCategoriesInternal() {
        return categoryRepository.findByParentIdIsNullAndActiveTrueOrderBySortOrder();
    }

    // ==================== МЕТОДЫ ДЛЯ АДМИНКИ ====================

    public List<Category> findAll() {
        return categoryRepository.findAllWithRelations();
    }

    public Optional<Category> findById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    public Category getById(Long id) {
        return categoryRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + id));
    }

    @Transactional
    public Category create(Map<String, Object> req) {
        String name = getString(req, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название категории обязательно");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(getString(req, "description"));
        category.setSortOrder(getInt(req));
        category.setActive(getBoolean(req));

        Long parentId = getLongByKey(req, "parentId");
        if (parentId != null) {
            Category parent = getById(parentId);
            category.setParent(parent);
        }

        Long categoryTypeId = getLongByKey(req, "categoryTypeId");
        if (categoryTypeId != null) {
            CategoryType ct = categoryTypeRepository.findById(categoryTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("CategoryType not found: " + categoryTypeId));
            category.setCategoryType(ct);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Map<String, Object> req) {
        Category category = getById(id);

        if (req.containsKey("name")) {
            String name = getString(req, "name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Название категории не может быть пустым");
            }
            category.setName(name);
        }

        if (req.containsKey("description")) {
            category.setDescription(getString(req, "description"));
        }

        if (req.containsKey("sortOrder")) {
            category.setSortOrder(getInt(req));
        }

        if (req.containsKey("isActive")) {
            category.setActive(getBoolean(req));
        }

        if (req.containsKey("parentId")) {
            Long parentId = getLongByKey(req, "parentId");
            if (parentId == null) {
                category.setParent(null);
            } else {
                category.setParent(getById(parentId));
            }
        }

        if (req.containsKey("categoryTypeId")) {
            Long ctId = getLongByKey(req, "categoryTypeId");
            if (ctId == null) {
                category.setCategoryType(null);
            } else {
                CategoryType ct = categoryTypeRepository.findById(ctId)
                        .orElseThrow(() -> new EntityNotFoundException("CategoryType not found: " + ctId));
                category.setCategoryType(ct);
            }
        }

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> searchByName(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        Category fallback = getOrCreateUncategorized();
        productRepository.moveProductsToAnotherCategory(category.getId(), fallback);
        categoryRepository.delete(category);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ PRIVATE МЕТОДЫ ====================

    private Category getOrCreateUncategorized() {
        return categoryRepository.findByNameIgnoreCase("Uncategorized")
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName("Uncategorized");
                    c.setDescription("Системная категория");
                    c.setSortOrder(0);
                    c.setActive(false);
                    return categoryRepository.save(c);
                });
    }

    private String getString(Map<String, Object> req, String key) {
        Object v = req.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> req) {
        Object v = req.get("sortOrder");
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) return Integer.parseInt(s);
        return 0;
    }

    private Boolean getBoolean(Map<String, Object> req) {
        Object v = req.get("isActive");
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return true;
    }

    private Long getLongByKey(Map<String, Object> req, String key) {
        Object v = req.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }
}
