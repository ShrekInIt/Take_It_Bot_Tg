package com.example.bot.Telegram_bot_take_it.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC-конфигурация Spring для:
 *  - раздачи статических ресурсов (css/js/images) из classpath
 *  - редиректа с корня сайта "/" на "/admin"
 *
 * @EnableWebMvc включает ручную настройку Spring MVC (без авто-настроек по умолчанию).
 */
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Настраивает правила раздачи статических ресурсов.
     * <p>
     * Пример:
     *  - запрос /css/style.css будет отдаваться из classpath:/static/css/style.css
     *
     * @param registry реестр, куда добавляются обработчики статических ресурсов
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/static/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    /**
     * Добавляет простой редирект без написания отдельного контроллера.
     * <p>
     * При заходе на "/" пользователь будет перенаправлен на "/admin".
     *
     * @param registry реестр view controller-ов
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/admin");
    }
}
