package com.example.bot.Telegram_bot_take_it.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, которое выбрасывается, когда запрашиваемый ресурс не найден.
 * <p>
 * Обычно используется в сервисах и контроллерах в ситуациях, когда:
 *  - не найден пользователь
 *  - не найден товар
 *  - не найден заказ
 *  - не найдена категория и т.п.
 * <p>
 * Как правило, перехватывается глобальным обработчиком ошибок (ExceptionHandler)
 * и преобразуется в HTTP-ответ 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
