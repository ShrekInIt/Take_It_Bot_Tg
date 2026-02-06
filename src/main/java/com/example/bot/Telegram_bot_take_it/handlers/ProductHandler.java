package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.service.KeyboardService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.example.bot.Telegram_bot_take_it.service.TelegramPhotoIdCacheService;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductHandler {

    private final TelegramPhotoIdCacheService telegramFileIdCacheService;
    private final KeyboardService keyboardService;
    private final ProductService productService;
    private final MessageSender messageSender;
    private final TelegramMessageSender telegramMessageSender;

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
                        String cacheKey = "product:" + product.getId();

                        String cachedFileId = telegramFileIdCacheService.get(cacheKey);
                        if (cachedFileId != null) {
                            SendResponse resp = telegramMessageSender.sendPhotoByFileId(
                                    chatId, cachedFileId, caption, keyboard, true
                            );

                            if (resp != null && resp.isOk()) {
                                return;
                            } else {
                                telegramFileIdCacheService.invalidate(cacheKey);
                            }
                        }

                        try {
                            byte[] photoBytes = keyboardService.readPhotoFile(product.getPhoto());

                            if (photoBytes != null && photoBytes.length > 0) {
                                SendResponse resp = telegramMessageSender.sendPhoto(
                                        chatId, photoBytes, caption, keyboard, true
                                );

                                if (resp != null && resp.isOk()
                                        && resp.message() != null
                                        && resp.message().photo() != null
                                        && resp.message().photo().length > 0) {

                                    var photos = resp.message().photo();
                                    var best = photos[0];
                                    for (var p : photos) {
                                        long bestSize = best.fileSize() == null ? 0 : best.fileSize();
                                        long curSize = p.fileSize() == null ? 0 : p.fileSize();
                                        if (curSize > bestSize) best = p;
                                    }

                                    telegramFileIdCacheService.put(cacheKey, best.fileId());
                                }
                                return;
                            }
                        } catch (Exception e) {
                            log.error("Ошибка отправки фото товара: {}", e.getMessage());
                        }
                    }

                    telegramMessageSender.sendMessageWithInlineKeyboardHtml(chatId, caption, keyboard, true);
                },
                () -> messageSender.sendMessage(chatId, "❌ Товар не найден")
        );
    }
}
