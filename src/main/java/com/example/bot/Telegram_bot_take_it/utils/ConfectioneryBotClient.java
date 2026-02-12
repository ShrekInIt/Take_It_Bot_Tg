package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.utils.interfaces.ConfectioneryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfectioneryBotClient implements ConfectioneryClient {
    private final RestTemplate restTemplate;

    @Value("${confectionery.bot.url:http://localhost:8081}")
    private String confectioneryBotUrl;

    @Value("${confectionery.bot.api-key:dev-key}")
    private String apiKey;

    @Override
    public void sendOrderToConfectionery(Object orderRequest) {
        String url = confectioneryBotUrl + "/api/orders/new";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);

        HttpEntity<Object> entity = new HttpEntity<>(orderRequest, headers);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

            log.info("✅ Заказ отправлен в кондитерский бот. status={}", resp.getStatusCode());

        } catch (HttpStatusCodeException e) {
            log.error("❌ Кондитерский бот вернул ошибку. status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;

        } catch (ResourceAccessException e) {
            log.error("❌ Не удалось достучаться до кондитерского бота (timeout/connection). url={}, err={}",
                    url, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка отправки заказа в кондитерский бот: {}", e.getMessage(), e);
            throw e;
        }
    }
}
