package com.example.bot.Telegram_bot_take_it.admin.controller.utilsController;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * MVC-контроллер страницы логина админ-панели.
 * <p>
 * Формирует сообщения для интерфейса (ошибка входа, выход из системы, запрет доступа),
 * используя параметры в URL, и возвращает шаблон страницы логина.
 */
@Controller
public class LoginController {

    /**
     * Открывает страницу логина админ-панели.
     * <p>
     * В зависимости от query-параметров добавляет в модель сообщения:
     *  - error -> "Неверный логин или пароль"
     *  - logout -> "Вы успешно вышли из системы"
     *  - denied -> "У вас нет доступа к этой странице"
     *
     * @param error  признак ошибки входа (если параметр есть в URL)
     * @param logout признак успешного выхода (если параметр есть в URL)
     * @param denied признак запрета доступа (если параметр есть в URL)
     * @param model  модель, в которую кладутся сообщения для отображения на странице
     * @return имя HTML-шаблона страницы логина "admin/login"
     */
    @GetMapping("/admin/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "denied", required = false) String denied,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        if (denied != null) {
            model.addAttribute("error", "У вас нет доступа к этой странице");
        }

        return "admin/login";
    }
}
