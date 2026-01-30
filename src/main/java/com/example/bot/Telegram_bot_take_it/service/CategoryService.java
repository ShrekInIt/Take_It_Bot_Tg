package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
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

    public void find(Long catId, Product product){
        try {
            Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id = " + catId));
            product.setCategory(category);
        } catch (Exception ex) {
            log.warn("Не удалось установить Category relation: {}", ex.getMessage());
        }
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
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
        category.setSortOrder(getInt(req, "sortOrder", 0));
        category.setActive(getBoolean(req, "isActive", true));

        Long parentId = getLong(req, "parentId");
        if (parentId != null) {
            Category parent = getById(parentId);
            category.setParent(parent);
        }

        return categoryRepository.save(category);
    }

    /* =========================
       UPDATE
     ========================= */

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
            category.setSortOrder(getInt(req, "sortOrder", 0));
        }

        if (req.containsKey("isActive")) {
            category.setActive(getBoolean(req, "isActive", true));
        }

        if (req.containsKey("parentId")) {
            Long parentId = getLong(req, "parentId");
            if (parentId == null) {
                category.setParent(null);
            } else {
                category.setParent(getById(parentId));
            }
        }

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> searchByName(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }

    /* =========================
       DELETE (SAFE)
     ========================= */

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);

        // 1️⃣ Получаем / создаём категорию "Uncategorized"
        Category fallback = getOrCreateUncategorized();

        // 2️⃣ Переносим товары
        productRepository.moveProductsToAnotherCategory(category.getId(), fallback.getId());

        // 3️⃣ Удаляем категорию
        categoryRepository.delete(category);
    }

    /* =========================
       HELPERS
     ========================= */

    private Category getOrCreateUncategorized() {
        return categoryRepository.findByNameIgnoreCase("Uncategorized")
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName("Uncategorized");
                    c.setDescription("Системная категория");
                    c.setSortOrder(0);
                    c.setActive(true);
                    return categoryRepository.save(c);
                });
    }

    private String getString(Map<String, Object> req, String key) {
        Object v = req.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> req, String key, Integer def) {
        Object v = req.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) return Integer.parseInt(s);
        return def;
    }

    private Boolean getBoolean(Map<String, Object> req, String key, Boolean def) {
        Object v = req.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    private Long getLong(Map<String, Object> req, String key) {
        Object v = req.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }
}
