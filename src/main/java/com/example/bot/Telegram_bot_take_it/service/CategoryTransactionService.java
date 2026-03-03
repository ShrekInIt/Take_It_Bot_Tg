package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.CategoryData;
import com.example.bot.Telegram_bot_take_it.dto.response.CategoryResponseDto;
import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.example.bot.Telegram_bot_take_it.mapper.ProductMapper;
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
    private final ProductMapper productMapper;

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
     * @see CategoryResponseDto
     * @see ProductResponseDto
     */
    @Transactional(readOnly = true)
    public CategoryData getCategoryData(Long categoryId) {
        CategoryResponseDto category = categoryService.getCategoryWithParentDto(categoryId);
        if (category == null) {
            return null;
        }

        List<CategoryResponseDto> subcategories = categoryService.getActiveSubcategoriesDto(categoryId);
        boolean hasProducts = productService.hasAvailableProductsInCategory(categoryId);

        List<ProductResponseDto> products = Collections.emptyList();
        if (hasProducts) {
            products = productService.getAvailableProductsWithStock(categoryId).stream()
                    .map(productMapper::toResponseDto)
                    .toList();
        }

        return new CategoryData(category, subcategories, hasProducts, products);
    }
}
