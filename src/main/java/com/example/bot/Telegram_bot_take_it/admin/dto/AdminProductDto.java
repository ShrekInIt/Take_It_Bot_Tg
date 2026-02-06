package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO товара для админ-панели.
 * <p>
 * Используется для отображения и редактирования товаров в админке.
 * Lombok @Data генерирует геттеры/сеттеры и служебные методы.
 * Lombok @Builder упрощает создание DTO.
 */
@Getter
@Setter
@Builder
public class AdminProductDto {

    /** ID товара */
    private Long id;

    /** Название товара */
    private String name;

    /** Цена/стоимость (по названию поля "amount" в проекте) */
    private Integer amount;

    /** Доступен ли товар для заказа */
    private Boolean available;

    /** Описание товара */
    private String description;

    /** Путь/URL основного фото товара */
    private String photo;

    /** ID категории товара */
    private Long categoryId;

    /** Название категории (для отображения в админке) */
    private String categoryName;

    /** Размер/вариант (если используется в проекте) */
    private String size;

    /** Количество/остаток товара (если используется) */
    private Integer count;
}
