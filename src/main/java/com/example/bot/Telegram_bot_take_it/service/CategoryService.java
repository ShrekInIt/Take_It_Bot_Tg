package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.repository.CategoryRepository;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Получить категорию по ID
     */
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId).orElse(null);
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
     */
    public InlineKeyboardMarkup createCombinedKeyboard(Long categoryId, List<Category> subcategories, List<com.example.bot.Telegram_bot_take_it.entity.Product> products) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        // Добавляем подкатегории с иконкой папки
        for (Category subcategory : subcategories) {
            InlineKeyboardButton button = new InlineKeyboardButton("📁 " + subcategory.getName())
                    .callbackData("category_" + subcategory.getId());
            keyboard.addRow(button);
        }

        // Добавляем товары с иконкой корзины
        for (com.example.bot.Telegram_bot_take_it.entity.Product product : products) {
            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId());

            keyboard.addRow(button);
        }

        // Добавляем кнопку "Назад"
        Category category = getCategoryById(categoryId);
        KeyboardService.method(keyboard, category);

        return keyboard;
    }

    /**
     * Получить активные подкатегории
     */
    public List<Category> getActiveSubcategories(Long parentId) {
        List<Category> subs = categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrder(parentId);

        subs.removeIf(cat -> "Добавки".equals(cat.getName()));

        return subs;
    }

    /**
     * Получить активные корневые категории
     */
    public List<Category> getActiveRootCategories() {
        return categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrder();
    }
}
