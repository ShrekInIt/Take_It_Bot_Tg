package com.example.bot.Telegram_bot_take_it.mapper;

import com.example.bot.Telegram_bot_take_it.dto.response.CategoryResponseDto;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.CategoryType;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponseDto toResponseDto(Category category) {
        return toResponseDto(category, false);
    }

    public CategoryResponseDto toResponseDto(Category category, boolean hasSubcategories) {
        if (category == null) {
            return null;
        }

        CategoryResponseDto.ParentInfo parentInfo = null;
        if (category.getParent() != null) {
            parentInfo = CategoryResponseDto.ParentInfo.builder()
                    .id(category.getParent().getId())
                    .name(category.getParent().getName())
                    .build();
        }

        CategoryResponseDto.CategoryTypeInfo typeInfo = toCategoryTypeInfo(category.getCategoryType());

        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .active(category.isActive())
                .hasSubcategories(hasSubcategories)
                .parent(parentInfo)
                .categoryType(typeInfo)
                .build();
    }

    private CategoryResponseDto.CategoryTypeInfo toCategoryTypeInfo(CategoryType type) {
        if (type == null) {
            return null;
        }

        return CategoryResponseDto.CategoryTypeInfo.builder()
                .id(type.getId())
                .code(type.getCode())
                .name(type.getName())
                .build();
    }
}

