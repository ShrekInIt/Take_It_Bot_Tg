package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductHandler {

    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final ProductService productService;
    private final MessageSender messageSender;

    /**
     * Обработка callback-запросов для товаров
     */
    public void handleProductCallback(Long chatId, String data) {
        String productPart = data.substring("product_".length());
        String[] parts = productPart.split("_");

        try {
            Long productId = Long.parseLong(parts[0]);
            Long sourceCategoryId = null;

            if (parts.length > 1 && !parts[1].isEmpty()) {
                sourceCategoryId = Long.parseLong(parts[1]);
            }

            log.info("Пользователь выбрал товар ID: {}, из категории: {}", productId, sourceCategoryId);
            showProductDetails(chatId, productId, sourceCategoryId);
        } catch (NumberFormatException e) {
            log.error("Ошибка в данных товара: {}", data);
            messageSender.sendMessage(chatId, "❌ Ошибка в данных товара");
        }
    }

    /**
     * Показать детали товара с фото и кнопками
     */
    private void showProductDetails(Long chatId, Long productId, Long sourceCategoryId) {
        productService.getProductById(productId).ifPresentOrElse(
                product -> {

                    messageSender.setLastMessageType(chatId);
                    String caption = MessageSender.getString(product.getAmount(), product, 1);

                    if (keyboardService.needsAddons(product)) {
                        caption += "\n\n<i>К этому напитку можно добавить сиропы или альтернативное молоко</i>";
                    }

                    InlineKeyboardMarkup keyboard = keyboardService.createProductKeyboard(product, 1, sourceCategoryId);

                    if (product.getPhoto() != null && !product.getPhoto().isEmpty()) {
                        try {
                            byte[] photoBytes = keyboardService.readPhotoFile(product.getPhoto());

                            if (photoBytes != null && photoBytes.length > 0) {
                                SendPhoto sendPhoto = new SendPhoto(chatId.toString(), photoBytes)
                                        .caption(caption)
                                        .parseMode(ParseMode.HTML)
                                        .replyMarkup(keyboard);

                                bot.execute(sendPhoto);
                                return;
                            }
                        } catch (Exception e) {
                            log.error("Ошибка отправки фото товара: {}", e.getMessage());
                        }
                    }

                    SendMessage message = new SendMessage(chatId.toString(), caption)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(keyboard);

                    bot.execute(message);
                },
                () -> messageSender.sendMessage(chatId, "❌ Товар не найден")
        );
    }
}
