package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.controller.commands.CommandDispatcher;
import com.example.bot.Telegram_bot_take_it.handlers.OrderHandler;
import com.example.bot.Telegram_bot_take_it.service.TelegramUserRegistrar;
import com.example.bot.Telegram_bot_take_it.service.UserAccessService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.SimpleRateLimiter;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.message.InaccessibleMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.concurrent.ConcurrentHashMap;

import static com.example.bot.Telegram_bot_take_it.utils.SimpleRateLimiter.WARN_COOLDOWN_MS;

@Controller
@RequiredArgsConstructor
public class BotController {
    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final CallbackHandlerController callbackHandler;
    private final OrderHandler orderHandler;
    private final TelegramUserRegistrar telegramUserRegistrar;
    private final CommandDispatcher commandDispatcher;
    private final MessageSender sendMessage;
    private final UserAccessService userAccessService;
    private final SimpleRateLimiter rateLimiter = new SimpleRateLimiter(5, 2000);

    private final ConcurrentHashMap<String, Long> lastWarnAt = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> lastSpamLogAt = new ConcurrentHashMap<>();
    private static final long SPAM_LOG_COOLDOWN_MS = 30_000;


    /**
     * Основной метод обработки входящих обновлений от Telegram
     * Распределяет обработку между сообщениями и callback-запросами
     */
    public void handleUpdate(Update update) {
        String telegramId = null;
        Long chatId = null;
        long t0 = System.nanoTime();
        if (update.message() != null) {
            telegramId = String.valueOf(update.message().from().id());
            chatId = update.message().chat().id();
            if(checkTimeAnswer(telegramId, chatId)){
                return;
            }

        } else if (update.callbackQuery() != null) {
            telegramId = String.valueOf(update.callbackQuery().from().id());

            var maybe = update.callbackQuery().maybeInaccessibleMessage();

            if (maybe != null && maybe.chat() != null) {
                chatId = maybe.chat().id();
                if(checkTimeAnswer(telegramId, chatId)){
                    return;
                }
            }
        }

        if (telegramId != null) {
            boolean active = userAccessService.isUserActiveByTelegramId(telegramId);

            if (!active) {
                if (chatId != null) {
                    sendMessage.sendBlockedMessage(chatId);
                }
                return;
            }
        }

        try {
            if (update.message() != null) {
                handleMessage(update.message());
            } else if (update.callbackQuery() != null) {
                handleCallbackQuery(update.callbackQuery());
            }
        } catch (Exception e) {
            logger.error("Error handling update: {}", e.getMessage(), e);
        }
        finally {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            logger.info("update took {} ms", ms);
        }
    }

    private boolean checkTimeAnswer(String telegramId, Long chatId) {
        if (telegramId != null && !rateLimiter.allow(telegramId)) {

            if (chatId != null && shouldWarn(telegramId)) {
                sendMessage.sendMessage(chatId, "⏳ Слишком часто. Подожди пару секунд.");
            }

            logSpam(telegramId, chatId);
            return true;
        }
        return false;
    }

    private boolean shouldWarn(String telegramId) {
        long now = System.currentTimeMillis();
        Long last = lastWarnAt.get(telegramId);

        if (last == null || now - last > WARN_COOLDOWN_MS) {
            lastWarnAt.put(telegramId, now);
            return true;
        }
        return false;
    }

    private void logSpam(String telegramId, Long chatId) {
        long now = System.currentTimeMillis();
        Long last = lastSpamLogAt.get(telegramId);

        if (last == null || now - last > SPAM_LOG_COOLDOWN_MS) {
            lastSpamLogAt.put(telegramId, now);
            logger.warn("Rate limit: telegramId={}, chatId={}", telegramId, chatId);
        }
    }

    /**
     * Обрабатывает входящее сообщение пользователя:
     * - регистрирует пользователя
     * - определяет команду
     * - передаёт управление нужному handler'у
     */
    private void handleMessage(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();
        com.pengrad.telegrambot.model.User telegramUser = message.from();
        Integer messageId = message.messageId();

        telegramUserRegistrar.touch(telegramUser, chatId);

        if (message.contact() != null) {
            logger.info("Получен контакт от chatId {}: {}", chatId, message.contact().phoneNumber());
            String phoneNumber = message.contact().phoneNumber();
            orderHandler.handleContact(chatId, phoneNumber, messageId);
            return;
        }

        if (text != null) {
            handleCommand(chatId, text, telegramUser, messageId);
        }
    }

    /**
     * Обрабатывает callback-запрос от inline-кнопок
     * и передаёт управление в CallbackHandlerController
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        User from = callbackQuery.from();

        com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage maybe = callbackQuery.maybeInaccessibleMessage();

        if (maybe instanceof Message msg) {
            telegramUserRegistrar.touch(from, msg.chat().id());
        } else if (maybe instanceof InaccessibleMessage im) {
            telegramUserRegistrar.touch(from, im.chat().id());
        }

        callbackHandler.handleCallbackQuery(callbackQuery);
    }

    /**
     * Обработка текстовых команд от пользователя
     */
    private void handleCommand(Long chatId, String text, com.pengrad.telegrambot.model.User telegramUser, Integer messageId) {
        String command = text.trim().toLowerCase();

        logger.info("Command received: {} from chatId: {}", command, chatId);

        boolean handled = commandDispatcher.dispatch(chatId, text, telegramUser);
        if (!handled) {
            orderHandler.handleTextMessage(chatId, text, messageId);
        }
    }
}