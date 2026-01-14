package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageCaption;
import com.pengrad.telegrambot.request.EditMessageText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuantityHandler {

    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final ProductService productService;
    private final MessageSender messageSender;

    /**
     * Обработка изменения количества товара
     */
    public void handleQuantityChange(Long chatId, Integer messageId, String callbackId, String data) {
        try {
            log.info("=== ОБРАБОТКА ИЗМЕНЕНИЯ КОЛИЧЕСТВА ===");
            log.info("Callback data: {}", data);
            log.info("Chat ID: {}, Message ID: {}", chatId, messageId);

            String[] parts = data.split("_");

            if (parts.length < 5) {
                log.error("Некорректный формат данных: {}", data);
                return;
            }

            String action = parts[1];
            Long productId = Long.parseLong(parts[2]);
            int currentQuantity = Integer.parseInt(parts[3]);
            Long sourceCategoryId = "null".equals(parts[4]) ? null : Long.parseLong(parts[4]);

            log.info("Действие: {}, Товар ID: {}, Текущее кол-во: {}", action, productId, currentQuantity);

            var productOpt = productService.getProductById(productId);
            if (productOpt.isEmpty()) {
                log.error("Товар с ID {} не найден", productId);
                messageSender.answerCallback(callbackId, "❌ Товар не найден");
                return;
            }

            Product product = productOpt.get();
            int newQuantity = currentQuantity;

            if ("plus".equals(action)) {
                if (product.getCount() != null && currentQuantity >= product.getCount()) {
                    String message = "⛔ В наличии только " + product.getCount() + " шт.";
                    messageSender.sendMessage(chatId, message);
                    log.warn(message);
                    messageSender.answerCallback(callbackId, message);
                    return;
                }
                newQuantity = Math.min(currentQuantity + 1, 99);
                log.info("Увеличиваем количество с {} до {}", currentQuantity, newQuantity);
            } else if ("minus".equals(action)) {
                newQuantity = Math.max(currentQuantity - 1, 1);
                log.info("Уменьшаем количество с {} до {}", currentQuantity, newQuantity);
            }

            if (newQuantity == currentQuantity) {
                log.info("Количество не изменилось, пропускаем обновление");
                return;
            }

            updateProductMessageWithSource(chatId, messageId, productId, newQuantity, sourceCategoryId);

            log.info("✅ Сообщение обновлено с новым количеством: {}", newQuantity);

        } catch (Exception e) {
            log.error("Ошибка обработки изменения количества: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка изменения количества");
        }
    }

    /**
     * Обновить сообщение с товаром с новым количеством и sourceCategoryId
     */
    private void updateProductMessageWithSource(Long chatId, Integer messageId, Long productId,
                                                int quantity, Long sourceCategoryId) {
        try {
            productService.getProductById(productId).ifPresent(product -> {
                String caption = MessageSender.getString(product.getAmount() * quantity, product, quantity);

                if (keyboardService.needsAddons(product)) {
                    caption += "\n\n<i>К этому напитку можно добавить сиропы или альтернативное молоко</i>";
                }

                InlineKeyboardMarkup keyboard = keyboardService.createProductKeyboard(product, quantity, sourceCategoryId);

                boolean editSuccess = false;

                try {
                    EditMessageCaption editCaption =
                            new EditMessageCaption(chatId, messageId)
                                    .caption(caption)
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(keyboard);

                    bot.execute(editCaption);
                    editSuccess = true;
                    log.info("Успешно обновлен caption товара");
                } catch (Exception e1) {
                    log.debug("Не удалось отредактировать caption товара, пробуем текст: {}", e1.getMessage());
                }

                if (!editSuccess) {
                    try {
                        EditMessageText editText = new EditMessageText(chatId, messageId, caption)
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(keyboard);

                        bot.execute(editText);
                        editSuccess = true;
                        log.info("Успешно обновлен текст товара");
                    } catch (Exception e2) {
                        log.error("Не удалось отредактировать текст товара: {}", e2.getMessage());
                    }
                }

                if (!editSuccess) {
                    log.warn("Не удалось отредактировать товар, заменяем сообщение");
                    messageSender.replaceMessage(chatId, messageId, caption, keyboard);
                }
            });
        } catch (Exception e) {
            log.error("Ошибка обновления товара: {}", e.getMessage());
        }
    }
}
