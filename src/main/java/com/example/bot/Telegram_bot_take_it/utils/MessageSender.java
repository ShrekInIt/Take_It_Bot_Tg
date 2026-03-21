package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.dto.response.ProductResponseDto;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSender {

    private final TelegramSendQueue sendQueue;

    private final Map<Long, String> lastMessageType = new ConcurrentHashMap<>();

    /**
     * Метод для установки типа сообщения
     */
    public void setLastMessageType(Long chatId) {
        lastMessageType.put(chatId, "product");
    }

    /**
     * Метод для получения предыдущего типа
     */
    public String getAndUpdateLastMessageType(Long chatId) {
        String previous = lastMessageType.put(chatId, "category");
        log.info("Last message type for chat {}: previous={}, new={}", chatId, previous, "category");
        return previous;
    }

    /**
     * Умное обновление сообщения: пробуем редактировать, учитывая тип сообщения
     * @param fromProduct true, если переходим из товара в категорию
     */
    public void smartUpdateMessage(Long chatId, Integer messageId, String newText,
                                    InlineKeyboardMarkup keyboard, boolean fromProduct) {
        try {
            if (fromProduct) {
                log.info("Переход из товара в категорию, заменяем сообщение");
                replaceMessage(chatId, messageId, newText, keyboard);
                return;
            }

            EditMessageText editMessage = new EditMessageText(chatId, messageId, newText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            sendQueue.enqueue(chatId, editMessage);

        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщения: {}", e.getMessage());
            replaceMessage(chatId, messageId, newText, keyboard);
        }
    }

    public void sendBlockedMessage(Long chatId) {
        String text = """
        🚫 Ваш аккаунт заблокирован.

        Пожалуйста, свяжитесь с администратором:
        📧 @barysheva_ol
        📞 +79930990947
        """;

        sendMessage(chatId, text);
    }

    /**
     * Удалить сообщение и отправить новое вместо редактирования
     */
    public void replaceMessage(Long chatId, Integer messageIdToDelete, String newText,
                                InlineKeyboardMarkup keyboard) {
        try {
            SendMessage newMessage = new SendMessage(chatId.toString(), newText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            sendQueue.enqueue(chatId, newMessage);
            log.info("Новое сообщение отправлено");

            log.info("Старое сообщение оставлено в истории: {}", messageIdToDelete);

            lastMessageType.remove(chatId);

        } catch (Exception e) {
            log.error("Ошибка замены сообщения: {}", e.getMessage());
            sendMessage(chatId, "❌ Ошибка обновления");
        }
    }

    /**
     * Отправить текстовое сообщение пользователю
     */
    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.Markdown);

        try {
            sendQueue.enqueue(chatId, message);
        } catch (Exception e) {
            log.error("Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Ответить на callback-запрос (подтвердить получение)
     */
    public void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);

        if (text != null && !text.isEmpty()) {
            answer.text(text);
        }

        try {
            sendQueue.enqueueGlobal(answer);
        } catch (Exception e) {
            log.error("Error answering callback: {}", e.getMessage());
        }
    }

    @NotNull
    public static String getString(long totalPrice, ProductResponseDto product, int quantity) {
        String description = product.getDescription() != null ? "\n" + product.getDescription() + "\n" : "";
        String size = product.getSize() != null ? product.getSize() : "Стандартный";
        long amount = product.getAmount() != null ? product.getAmount() : 0L;

        return String.format(
                "<b>%s</b>\n%s\n\n<b>Цена:</b> %d₽\n<b>Размер:</b> %s\n<b>Количество:</b> %d шт.\n<b>Итого:</b> <b>%d₽</b>",
                product.getName(),
                description,
                amount,
                size,
                quantity,
                totalPrice
        );
    }
}
