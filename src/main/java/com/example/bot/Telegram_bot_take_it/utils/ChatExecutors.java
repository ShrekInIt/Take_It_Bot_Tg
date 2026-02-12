package com.example.bot.Telegram_bot_take_it.utils;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ChatExecutors {
    private final ExecutorService[] pools;

    public ChatExecutors(@Value("${bot.chat-executors.stripes:4}") int stripes) {
        pools = new ExecutorService[stripes];
        for (int i = 0; i < stripes; i++) {
            pools[i] = Executors.newSingleThreadExecutor();
        }
    }

    public Executor forChat(long chatId) {
        int idx = (int) (Math.abs(chatId) % pools.length);
        return pools[idx];
    }

    @PreDestroy
    public void shutdown() {
        for (ExecutorService pool : pools) {
            pool.shutdown();
        }
    }
}
