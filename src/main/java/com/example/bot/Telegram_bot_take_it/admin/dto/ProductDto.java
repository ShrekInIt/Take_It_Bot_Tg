package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Упрощённый DTO товара.
 * <p>
 * Обычно используется как вложенный объект внутри OrderItemDto,
 * чтобы не тянуть весь товар целиком — только id и name.
 */
@Getter
@Setter
public class ProductDto {
    private Long id;

    private String name;

    /**
     * Пустой конструктор — нужен для сериализации/десериализации (Jackson).
     */
    public ProductDto(){}

    /**
     * Конструктор для быстрого создания ProductDto.
     *
     * @param id   ID товара
     * @param name название товара
     */
    public ProductDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
