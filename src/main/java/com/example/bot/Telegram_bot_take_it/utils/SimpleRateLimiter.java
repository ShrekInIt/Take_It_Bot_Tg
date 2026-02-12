package com.example.bot.Telegram_bot_take_it.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleRateLimiter {
    private final long windowMs;
    private final int maxRequests;
    public static final long WARN_COOLDOWN_MS = 10_000;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public SimpleRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();

        Window w = windows.compute(key, (k, old) -> {
            if (old == null || now - old.windowStartMs >= windowMs) {
                return new Window(now, 1, now);
            }
            old.count++;
            old.lastSeenMs = now;
            return old;
        });


        cleanupOccasionally(now);

        return w.count <= maxRequests;
    }

    private volatile long lastCleanupMs = 0;
    private final long ttlMs = 60 * 60_000;

    private void cleanupOccasionally(long now) {
        long cleanupEveryMs = 60_000;
        if (now - lastCleanupMs < cleanupEveryMs) return;
        lastCleanupMs = now;

        windows.entrySet().removeIf(e -> now - e.getValue().lastSeenMs > ttlMs);
    }

    private static class Window {
        final long windowStartMs;
        int count;
        volatile long lastSeenMs;

        Window(long windowStartMs, int count, long lastSeenMs) {
            this.windowStartMs = windowStartMs;
            this.count = count;
            this.lastSeenMs = lastSeenMs;
        }
    }
}
