package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.CategoryType;
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

    public List<Category> findAll() {
        return categoryRepository.findAllWithRelations();
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
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
