package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCategoryDto {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;     // corresponds to sort_order
    private Long parentId;
    private String parentName;
    private Boolean isActive;
    private Long categoryTypeId;   // если используешь category_type
    private String categoryTypeName;
}
