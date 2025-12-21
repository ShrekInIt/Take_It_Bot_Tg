package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Получить доступные продукты в категории с количеством > 0
     */
    public List<Product> getAvailableProductsWithStock(Long categoryId) {
        return productRepository.findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(categoryId);
    }

    /**
     * Проверить, есть ли доступные продукты с количеством > 0 в категории
     */
    public boolean hasAvailableProductsInCategory(Long categoryId) {
        return !productRepository.findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(categoryId).isEmpty();
    }

    /**
     * Получить продукт по ID
     */
    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    /**
     * Получить доступные сиропы
     */
    public List<Product> getAvailableSyrups() {
        Long syrupCategoryId = 20L;
        return getAvailableProductsWithStock(syrupCategoryId);
    }

    /**
     * Получить доступное альтернативное молоко
     */
    public List<Product> getAvailableMilks() {
        Long milkCategoryId = 21L;
        return getAvailableProductsWithStock(milkCategoryId);
    }
}
