package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {
    private Long id;

    private String name;

    public ProductDto(){}

    public ProductDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
