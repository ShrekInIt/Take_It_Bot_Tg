package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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
     * Создать комбинированную клавиатуру (подкатегории + товары)
     * @Transactional для безопасного доступа к полям категории
     */
    @Transactional(readOnly = true)
    public InlineKeyboardMarkup createCombinedKeyboard(Long categoryId, List<Category> subcategories, List<com.example.bot.Telegram_bot_take_it.entity.Product> products) {
        log.info("=== CREATE COMBINED KEYBOARD ===");
        log.info("Category ID: {}", categoryId);
        log.info("Number of subcategories passed: {}", subcategories.size());
        log.info("Number of products passed: {}", products.size());
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Category subcategory : subcategories) {
            InlineKeyboardButton button = new InlineKeyboardButton("📁 " + subcategory.getName())
                    .callbackData("category_" + subcategory.getId());
            keyboard.addRow(button);
        }

        for (com.example.bot.Telegram_bot_take_it.entity.Product product : products) {
            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId() + "_" + categoryId);

            keyboard.addRow(button);
        }

        Category category = getCategoryWithParent(categoryId);
        if (category != null) {
            KeyboardService.addBackButton(keyboard, category);
        }

        return keyboard;
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
        Category category = getCategoryById(categoryId);
        if (category == null) {
            return false;
        }

        // Список ID категорий кофе и всех его подкатегорий
        Set<Long> coffeeCategoryIds = Set.of(
                3L,    // Кофе
                5L,    // Капучино
                6L,    // Раф
                7L,    // Раф авторский
                8L,    // Капучино(200)
                9L,    // Раф(200)
                10L,   // Раф авторский(200)
                11L,   // Капучино(300)
                12L,   // Раф(400)
                13L,   // Раф авторский(400)
                19L    // Добавки
        );

        return coffeeCategoryIds.contains(categoryId);
    }

    /**
     * Получить активные корневые категории
     */
    @Transactional(readOnly = true)
    public List<Category> getActiveRootCategories() {
        return categoryRepository.findByParentIdIsNullAndActiveTrueOrderBySortOrder();
    }
}
