package com.example.bot.Telegram_bot_take_it;

import com.example.bot.Telegram_bot_take_it.controller.BotController;
import com.example.bot.Telegram_bot_take_it.utils.ChatExecutors;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotInitializer {
    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final TelegramBot bot;
    private final BotController botController;
    private final ChatExecutors chatExecutors;

    @PostConstruct
    public void init() {
        log.info("🚀 Telegram Bot STARTED");

        bot.setUpdatesListener(updates -> {
            for (var update : updates) {
                long chatId = extractChatId(update);

                if (chatId == 0L) {
                    botController.handleUpdate(update);
                    continue;
                }

                chatExecutors.forChat(chatId).execute(() -> botController.handleUpdate(update));
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("✅ Telegram bot is listening");
    }

    private long extractChatId(Update update) {
        if (update == null) return 0L;


        if (update.message() != null && update.message().chat() != null) {
            return update.message().chat().id();
        }

        if (update.callbackQuery() != null) {
            MaybeInaccessibleMessage maybe = update.callbackQuery().maybeInaccessibleMessage();
            if (maybe != null && maybe.messageId()!= null && maybe.chat() != null) {
                return maybe.chat().id();
            }
        }

        return 0L;
    }

}
