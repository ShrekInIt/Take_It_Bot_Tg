package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.entity.ProductAddon;
import com.example.bot.Telegram_bot_take_it.repository.ProductAddonRepository;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductAddonService {

    private final ProductAddonRepository productAddonRepository;
    private final ProductRepository productRepository;

    /**
     * Получить доступные добавки для продукта сгруппированные по категориям
     */
    public Map<Long, List<Product>> getAvailableAddonsByCategory(Long productId) {
        // Получаем все записи о добавках для продукта
        List<ProductAddon> addons = productAddonRepository.findByProductId(productId);

        // Фильтруем только активные добавки
        return addons.stream()
                .filter(ProductAddon::getIsActive)
                .map(ProductAddon::getAddonProduct)
                .filter(product -> product.getAvailable() && product.getCount() > 0)
                .collect(Collectors.groupingBy(Product::getCategoryId));
    }

    /**
     * Проверить, нужны ли добавки для продукта
     */
    public boolean needsAddons(Product product) {
        if (product == null) return false;

        // Проверяем, есть ли записи о добавках для этого продукта
        List<ProductAddon> addons = productAddonRepository.findByProductId(product.getId());
        return !addons.isEmpty();
    }

    /**
     * Получить добавки по категории для продукта
     */
    public List<Product> getAddonsByCategory(Long productId, Long categoryId) {
        return productAddonRepository.findByProductIdAndAddonProduct_CategoryId(productId, categoryId).stream()
                .filter(ProductAddon::getIsActive)
                .map(ProductAddon::getAddonProduct)
                .filter(product -> product.getAvailable() && product.getCount() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Получить цену добавки для конкретного продукта
     */
    public Integer getAddonPrice(Long productId, Long addonProductId) {
        return productAddonRepository.findByProductIdAndAddonProductId(productId, addonProductId)
                .map(ProductAddon::getAdditionalPrice)
                .orElse(0);
    }
}
