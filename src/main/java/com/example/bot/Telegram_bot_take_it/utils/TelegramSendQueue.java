package com.example.bot.Telegram_bot_take_it.utils;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramSendQueue {
    private final TelegramBot bot;

    /**
     * Максимум задач "в ожидании" на один чат.
     * Если пользователь заспамит кнопками — не раздуем память.
     */
    private static final int MAX_PENDING_PER_CHAT = 50;

    /**
     * IO пул для сетевых запросов к Telegram.
     * ВАЖНО: bounded queue + CallerRunsPolicy => память не раздувается,
     * при перегрузе обработчик апдейта сам начнёт "притормаживать".
     */
    private final ThreadPoolExecutor ioPool = new ThreadPoolExecutor(
            4, 4,
            30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            r -> {
                Thread t = new Thread(r);
                t.setName("tg-io");
                t.setDaemon(false);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // "Хвост" очереди по чату — сохраняем порядок отправок внутри одного чата
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    // Сколько задач сейчас "в ожидании" на чат
    private final ConcurrentHashMap<Long, AtomicInteger> inQueue = new ConcurrentHashMap<>();

    public <T extends BaseResponse> CompletableFuture<T> enqueue(Long chatId, BaseRequest<?, T> req) {
        CompletableFuture<T> result = new CompletableFuture<>();

        AtomicInteger cnt = inQueue.computeIfAbsent(chatId, k -> new AtomicInteger(0));
        int cur = cnt.incrementAndGet();
        if (cur > MAX_PENDING_PER_CHAT) {
            cnt.decrementAndGet();
            if (cnt.get() == 0) inQueue.remove(chatId, cnt);
            result.completeExceptionally(new RejectedExecutionException(
                    "Too many pending telegram requests for chatId=" + chatId
            ));
            return result;
        }

        tails.compute(chatId, (id, tail) -> {
            CompletableFuture<Void> prev = (tail == null)
                    ? CompletableFuture.completedFuture(null)
                    : tail;

            CompletableFuture<Void> next = prev.handle((v, ex) -> null)
                    .thenRunAsync(() -> {
                        try {
                            T resp = bot.execute(req);
                            if (!resp.isOk()) {
                                log.warn("Telegram error {}: {}", resp.errorCode(), resp.description());
                            }
                            result.complete(resp);
                        } catch (Throwable e) {
                            result.completeExceptionally(e);
                        } finally {
                            int left = cnt.decrementAndGet();
                            if (left == 0) inQueue.remove(id, cnt);
                        }
                    }, ioPool);

            next.whenComplete((v, ex) -> {
                CompletableFuture<Void> curTail = tails.get(id);
                if (curTail == next) tails.remove(id, next);
            });

            return next;
        });

        return result;
    }

    /**
     * Для запросов без chatId (например, AnswerCallbackQuery).
     * Тут обычно очередь не нужна, просто отправляем в IO пул.
     */
    public <T extends BaseResponse> void enqueueGlobal(BaseRequest<?, T> req) {
        CompletableFuture.supplyAsync(() -> {
            T resp = bot.execute(req);
            if (!resp.isOk()) {
                log.warn("Telegram error {}: {}", resp.errorCode(), resp.description());
            }
            return resp;
        });
    }

    @PreDestroy
    public void shutdown() {
        ioPool.shutdown();
    }
}
