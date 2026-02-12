package com.example.bot.Telegram_bot_take_it;

import com.pengrad.telegrambot.model.Update;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BotControllerLoadTest {
    @Test
    void shouldHandleUpdatesWith5ConcurrentUsers_mockedDeps() throws Exception {
        var telegramUserRegistrar = Mockito.mock(com.example.bot.Telegram_bot_take_it.service.TelegramUserRegistrar.class);
        var userAccessService = Mockito.mock(com.example.bot.Telegram_bot_take_it.service.UserAccessService.class);
        var callbackHandlerController = Mockito.mock(com.example.bot.Telegram_bot_take_it.controller.CallbackHandlerController.class);
        var orderHandler = Mockito.mock(com.example.bot.Telegram_bot_take_it.handlers.OrderHandler.class);
        var commandDispatcher = Mockito.mock(com.example.bot.Telegram_bot_take_it.controller.commands.CommandDispatcher.class);
        var messageSender = Mockito.mock(com.example.bot.Telegram_bot_take_it.utils.MessageSender.class);

        var botController = new com.example.bot.Telegram_bot_take_it.controller.BotController(
                callbackHandlerController,
                orderHandler,
                telegramUserRegistrar,
                commandDispatcher,
                messageSender,
                userAccessService
        );

        Mockito.when(userAccessService.isUserActiveByTelegramId(Mockito.anyString()))
                .thenReturn(true);

        Mockito.doNothing().when(orderHandler).handleTextMessage(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(callbackHandlerController).handleCallbackQuery(Mockito.any());

        int users = 5;
        int perUser = 200;
        int total = users * perUser;


        List<Update> updates = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            updates.add(new Update());
        }

        ExecutorService pool = Executors.newFixedThreadPool(users);
        try {
            List<Long> latencies = new CopyOnWriteArrayList<>();
            AtomicInteger errors = new AtomicInteger(0);

            CountDownLatch startGun = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int u = 0; u < users; u++) {
                final int offset = u * perUser;
                futures.add(pool.submit(() -> {
                    try {
                        startGun.await();
                        for (int j = 0; j < perUser; j++) {
                            Update up = updates.get(offset + j);
                            long t0 = System.nanoTime();
                            try {
                                botController.handleUpdate(up);
                            } catch (Throwable e) {
                                errors.incrementAndGet();
                            } finally {
                                latencies.add(System.nanoTime() - t0);
                            }
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            long wallStart = System.nanoTime();
            startGun.countDown();

            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
            long wallNs = System.nanoTime() - wallStart;

            LatencyStats stats = LatencyStats.fromNanos(latencies);

            System.out.println("Total updates: " + total);
            System.out.println("Errors: " + errors.get());
            System.out.println("Throughput (updates/sec): " + (total / (wallNs / 1_000_000_000.0)));
            System.out.println(stats);

            assertThat(errors.get()).isEqualTo(0);

            assertThat(stats.p95Millis).isLessThan(50.0);
        } finally {
            pool.shutdownNow();
        }
    }

        record LatencyStats(double p50Millis, double p95Millis, double p99Millis) {

        static LatencyStats fromNanos(List<Long> nanos) {
                if (nanos.isEmpty()) return new LatencyStats(0, 0, 0);
                long[] arr = nanos.stream().mapToLong(Long::longValue).sorted().toArray();
                return new LatencyStats(
                        percentile(arr, 0.50) / 1_000_000.0,
                        percentile(arr, 0.95) / 1_000_000.0,
                        percentile(arr, 0.99) / 1_000_000.0
                );
            }

            static long percentile(long[] sorted, double p) {
                int idx = (int) Math.ceil(p * sorted.length) - 1;
                idx = Math.max(0, Math.min(idx, sorted.length - 1));
                return sorted[idx];
            }

            @NotNull
            @Override
            public String toString() {
                return "Latency ms: p50=" + p50Millis + " p95=" + p95Millis + " p99=" + p99Millis;
            }
        }
}
