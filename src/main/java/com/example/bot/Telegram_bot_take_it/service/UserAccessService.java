package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccessService {
    private final UserRepository userRepository;

    public boolean isUserActiveByTelegramId(String telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(User::isActive)
                .orElse(true);
    }
}
