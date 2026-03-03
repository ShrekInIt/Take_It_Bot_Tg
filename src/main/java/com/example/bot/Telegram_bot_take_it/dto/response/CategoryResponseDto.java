package com.example.bot.Telegram_bot_take_it.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа с данными категории.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean active;
    private Boolean hasSubcategories;

    private ParentInfo parent;
    private CategoryTypeInfo categoryType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTypeInfo {
        private Long id;
        private String code;
        private String name;
    }
}

