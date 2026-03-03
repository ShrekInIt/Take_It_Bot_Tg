package com.example.bot.Telegram_bot_take_it.mapper;

import com.example.bot.Telegram_bot_take_it.dto.request.CreateProductRequest;
import com.example.bot.Telegram_bot_take_it.dto.request.UpdateProductRequest;
import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper для преобразования Product Entity в DTO и обратно
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryService categoryService;

    /**
     * Преобразует Product Entity в ProductResponseDto
     */
    public ProductResponseDto toResponseDto(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponseDto.CategoryInfo categoryInfo = null;
        if (product.getCategory() != null) {
            categoryInfo = ProductResponseDto.CategoryInfo.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .build();
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .amount(product.getAmount())
                .size(product.getSize())
                .count(product.getCount())
                .available(product.getAvailable())
                .photo(product.getPhoto())
                .description(product.getDescription())
                .category(categoryInfo)
                .build();
    }

    /**
     * Создает новый Product Entity из CreateProductRequest
     */
    public Product toEntity(CreateProductRequest request) {
        if (request == null) {
            return null;
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setAmount(request.getAmount());
        product.setSize(request.getSize());
        product.setCount(request.getCount() != null ? request.getCount() : 0);
        product.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        product.setDescription(request.getDescription());
        product.setPhoto(request.getPhoto());

        // Загружаем категорию
        if (request.getCategoryId() != null) {
            Category category = categoryService.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Категория с ID " + request.getCategoryId() + " не найдена"
                    ));
            product.setCategory(category);
        }

        return product;
    }

    /**
     * Обновляет существующий Product Entity из UpdateProductRequest
     * Обновляются только поля, которые присутствуют в request (не null)
     */
    public void updateEntity(Product product, UpdateProductRequest request) {
        if (product == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }

        if (request.getAmount() != null) {
            product.setAmount(request.getAmount());
        }

        if (request.getSize() != null) {
            product.setSize(request.getSize());
        }

        if (request.getCount() != null) {
            product.setCount(request.getCount());
        }

        if (request.getAvailable() != null) {
            product.setAvailable(request.getAvailable());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getPhoto() != null) {
            product.setPhoto(request.getPhoto());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryService.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Категория с ID " + request.getCategoryId() + " не найдена"
                    ));
            product.setCategory(category);
        }
    }
}

