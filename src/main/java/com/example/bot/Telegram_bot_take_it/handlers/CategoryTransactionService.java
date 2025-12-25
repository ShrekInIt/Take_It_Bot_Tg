package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.dto.CategoryData;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryTransactionService {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Получает полные данные категории в рамках одной транзакции только для чтения
     *
     * <p>Метод выполняет следующие действия в одной транзакции:</p>
     * <ul>
     *   <li>Получает категорию с информацией о родительской категории</li>
     *   <li>Получает список активных подкатегорий</li>
     *   <li>Проверяет наличие доступных товаров в категории</li>
     *   <li>При наличии товаров загружает их с информацией о наличии на складе</li>
     * </ul>
     *
     * <p><b>Особенности транзакции:</b></p>
     * <ul>
     *   <li><code>readOnly = true</code> - транзакция только для чтения, повышает производительность</li>
     *   <li>Все операции с БД выполняются в одной транзакции</li>
     *   <li>Избегает проблем с lazy loading и открытыми сессиями</li>
     *   <li>Оптимизирована для чтения данных для отображения в UI</li>
     * </ul>
     *
     * @param categoryId ID категории для загрузки данных
     * @return {@link CategoryData} объект с данными категории, подкатегорий и товаров,
     *         или {@code null} если категория не найдена
     * @see CategoryData
     * @see Category
     * @see Product
     */
    @Transactional(readOnly = true)
    public CategoryData getCategoryData(Long categoryId) {
        Category category = categoryService.getCategoryWithParent(categoryId);
        if (category == null) {
            return null;
        }

        List<Category> subcategories = categoryService.getActiveSubcategories(categoryId);
        boolean hasProducts = productService.hasAvailableProductsInCategory(categoryId);

        List<Product> products = Collections.emptyList();
        if (hasProducts) {
            products = productService.getAvailableProductsWithStock(categoryId);
        }

        return new CategoryData(category, subcategories, hasProducts, products);
    }
}
