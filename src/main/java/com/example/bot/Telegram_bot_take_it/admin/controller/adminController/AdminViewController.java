package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC-контроллер для открытия стартовой страницы админ-панели.
 * <p>
 * В отличие от REST-контроллеров, этот контроллер возвращает имя HTML-шаблона,
 * который будет отрендерен на сервере (например, Thymeleaf).
 */
@Controller
public class AdminViewController {

    /**
     * Открывает страницу админки по адресу /admin.
     *
     * @return имя шаблона "index" (какую страницу показать пользователю)
     */
    @GetMapping("/admin")
    public String adminHome() {
        return "index";
    }
}
