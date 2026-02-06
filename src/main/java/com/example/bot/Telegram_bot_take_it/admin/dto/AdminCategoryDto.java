package com.example.bot.Telegram_bot_take_it.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO для передачи информации о категории в админ-панель.
 * <p>
 * Используется для отображения/редактирования категорий в интерфейсе админки.
 * Lombok @Data генерирует геттеры/сеттеры, equals/hashCode и toString.
 * Lombok @Builder позволяет удобно собирать DTO через билдера.
 */
@Getter
@Setter
@Builder
public class AdminCategoryDto {

    /** ID категории */
    private Long id;

    /** Название категории */
    private String name;

    /** Описание категории */
    private String description;

    /** Порядок сортировки категории в списках (чем меньше — тем выше) */
    private Integer sortOrder;

    /** ID родительской категории (если категория вложенная) */
    private Long parentId;

    /** Название родительской категории (для отображения в админке) */
    private String parentName;

    /** Активна ли категория (используется ли в приложении) */
    private Boolean isActive;

    /** ID типа категории (если есть классификация по типам) */
    private Long categoryTypeId;

    /** Название типа категории (для отображения в админке) */
    private String categoryTypeName;
}
