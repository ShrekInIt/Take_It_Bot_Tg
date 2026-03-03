package com.example.bot.Telegram_bot_take_it.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления существующего продукта
 * Все поля опциональны - обновляются только переданные
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(min = 1, max = 255, message = "Название должно быть от 1 до 255 символов")
    private String name;

    private Long categoryId;

    @Min(value = 0, message = "Цена не может быть отрицательной")
    private Long amount;

    @Size(max = 50, message = "Размер не может быть больше 50 символов")
    private String size;

    @Min(value = 0, message = "Количество не может быть отрицательным")
    private Integer count;

    private Boolean available;

    @Size(max = 1000, message = "Описание не может быть больше 1000 символов")
    private String description;

    @Size(max = 500, message = "URL фото не может быть больше 500 символов")
    private String photo;
}

