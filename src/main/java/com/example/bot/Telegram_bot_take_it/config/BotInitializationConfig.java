package com.example.bot.Telegram_bot_take_it.config;

import com.example.bot.Telegram_bot_take_it.admin.service.BotUpdateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class BotInitializationConfig {
    private final BotUpdateService botUpdateService;

    @PostConstruct
    public void initBot() {
        try {
            Thread.sleep(3000);
            botUpdateService.startBot();
        } catch (Exception e) {
            System.err.println("Failed to initialize bot: " + e.getMessage());
        }
    }
}
