package com.example.bot.Telegram_bot_take_it.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация Spring для создания и регистрации RestTemplate.
 * <p>
 * RestTemplate используется для выполнения HTTP-запросов к внешним сервисам
 * (GET/POST/PUT/DELETE) изнутри приложения.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Регистрирует RestTemplate как Spring Bean.
     * <p>
     * Благодаря этому RestTemplate можно внедрять через @Autowired / constructor injection
     * в любых сервисах/компонентах, где нужно делать HTTP-запросы.
     *
     * @return новый экземпляр RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
