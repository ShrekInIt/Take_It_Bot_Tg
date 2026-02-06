package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.OrderSession;
import com.example.bot.Telegram_bot_take_it.repository.OrderSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Сервис управления сессией оформления заказа (замена in-memory Map).
 */
@Service
@RequiredArgsConstructor
public class OrderSessionService {
    private final OrderSessionRepository repo;

    /** Получить сессию оформления заказа пользователя */
    public Optional<OrderSession> get(Long chatId) {
        return repo.findById(chatId);
    }

    /** Создать новую сессию (или перезаписать), если начинается оформление */
    public OrderSession createOrReset(Long chatId) {
        OrderSession s = OrderSession.builder()
                .chatId(chatId)
                .updatedAt(Instant.now())
                .build();
        return repo.save(s);
    }

    /** Сохранить изменения сессии */
    public OrderSession save(OrderSession session) {
        session.setUpdatedAt(Instant.now());
        return repo.save(session);
    }

    /** Завершить/очистить сессию */
    public void clear(Long chatId) {
        repo.deleteById(chatId);
    }
}
