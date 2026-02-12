package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserAccessService {
    private final UserRepository userRepository;

    private final Cache<String, Boolean> activeCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(60))
                    .maximumSize(10_000)
                    .build();

    public boolean isUserActiveByTelegramId(String telegramId) {
        return activeCache.get(telegramId, id ->
                userRepository.findByTelegramId(id).map(User::isActive).orElse(true)
        );
    }
}
