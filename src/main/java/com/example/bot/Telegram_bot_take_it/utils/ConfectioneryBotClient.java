package com.example.bot.Telegram_bot_take_it.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfectioneryBotClient {
    private final RestTemplate restTemplate;

    @Value("${confectionery.bot.url:http://localhost:8081}")
    private String confectioneryBotUrl;

    /**
     * Отправить заказ в кондитерский бот
     */
    public void sendOrderToConfectionery(Object orderRequest) {
        try {
            String url = confectioneryBotUrl + "/api/orders/new";
            restTemplate.postForObject(url, orderRequest, String.class);
            log.info("✅ Заказ отправлен в кондитерский бот");
        } catch (Exception e) {
            log.error("❌ Ошибка отправки заказа в кондитерский бот: {}", e.getMessage());
        }
    }
}
