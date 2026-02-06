package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для отображения HTML-страниц админ-панели.
 * <p>
 * В отличие от REST-контроллеров, этот класс возвращает имена шаблонов (страниц),
 * которые затем рендерятся сервером (например, через Thymeleaf).
 * <p>
 * Используется для навигации по админскому интерфейсу.
 */
@Controller
public class AdminPageController {

    /**
     * Открывает страницу управления пользователями.
     * <p>
     * Обычно содержит список пользователей и их данные.
     *
     * @return имя HTML-шаблона страницы пользователей
     */
    @GetMapping("/admin/users")
    public String usersPage() {
        return "admin/users";
    }

    /**
     * Открывает главную страницу админ-панели.
     * <p>
     * Обычно это dashboard со статистикой, заказами и пользователями.
     *
     * @return имя HTML-шаблона главной страницы админки
     */
    @GetMapping("/admin/dashboard")
    public String dashboardPage() {
        return "admin/dashboard";
    }

    /**
     * Открывает страницу управления категориями.
     * <p>
     * Используется для создания, редактирования и удаления категорий товаров.
     *
     * @return имя HTML-шаблона страницы категорий
     */
    @GetMapping("/admin/categories")
    public String categoriesPage() {
        return "admin/categories";
    }

    /**
     * Открывает страницу управления администраторами.
     * <p>
     * Здесь ADMIN может добавлять, редактировать и удалять продукты.
     *
     * @return имя HTML-шаблона страницы администраторов
     */
    @GetMapping("/admin/products")
    public String productsPage() {
        return "admin/products";
    }

    /**
     * Открывает страницу управления заказами.
     * <p>
     * Здесь SUPER_ADMIN может:
     *  - просматривать заказы
     *  - менять статусы
     *  - искать заказы по пользователю
     *
     * @return имя HTML-шаблона страницы заказов
     */
    @GetMapping("/admin/orders")
    public String ordersPage() {
        return "admin/orders";
    }

    /**
     * Открывает страницу управления администраторами.
     * <p>
     * Здесь SUPER_ADMIN может добавлять, редактировать и удалять админов.
     *
     * @return имя HTML-шаблона страницы администраторов
     */
    @GetMapping("/admin/admins")
    public String adminsPage() {
        return "admin/admins";
    }
}
