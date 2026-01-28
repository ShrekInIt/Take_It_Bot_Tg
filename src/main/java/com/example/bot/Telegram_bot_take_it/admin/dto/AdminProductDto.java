package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminProductDto {
    private Long id;
    private String name;
    private Integer amount;
    private Boolean available;
    private String description;
    private String photo;
    private Long categoryId;
    private String categoryName;
    private String size;
    private Integer count;
}
