package com.example.bot.Telegram_bot_take_it.admin.controller.utilsController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурационный класс Spring MVC для раздачи статических ресурсов.
 * <p>
 * Настраивает правила (resource handlers), чтобы приложение могло отдавать:
 *  - JavaScript-файлы по пути /js/**
 *  - загруженные файлы (uploads) по пути /uploads/**
 * <p>
 * Реализует WebMvcConfigurer, чтобы добавить свои правила в конфигурацию MVC.
 */
@Configuration
public class StaticResourceController implements WebMvcConfigurer {

    /**
     * Путь к директории загрузок, берётся из application.properties / application.yml:
     * file.upload-dir=...
     */
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Регистрирует обработчики статических ресурсов.
     * <p>
     * 1) /js/** берётся из classpath (ресурсы внутри приложения)
     * 2) /uploads/** берётся с файловой системы:
     *    - из uploadDir (из конфигурации)
     *    - также указан жёстко прописанный путь Windows
     * <p>
     * Важно: здесь /uploads/** добавлен несколько раз — Spring обработает их в порядке добавления.
     *
     * @param registry реестр, в который добавляются правила раздачи статических ресурсов
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/", "classpath:/js/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:C:/Users/artyo/OneDrive/Рабочий стол/Take_it/бот/uploads/");
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
