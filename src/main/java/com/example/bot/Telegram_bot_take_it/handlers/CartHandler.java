package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CartItemAddonService;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartHandler {

    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final ProductService productService;
    private final CartService cartService;
    private final CartItemAddonService cartItemAddonService;
    private final MessageSender messageSender;

    /**
     * Обработка callback запросов для корзины
     */
    public void handlerCartCallback(Long chatId, String callbackId, String data){
        if (data.startsWith("cart_add_")) {
            log.info("Обработка добавления в корзину...");
            handleAddToCart(chatId, callbackId, data);
        }
        else if (data.startsWith("cart_add_with_addon_")) {
            log.info("Обработка добавления в корзину с добавкой...");
            handleAddToCartWithAddon(chatId, callbackId, data);
        }
        else if (data.startsWith("cart_clear")) {
            log.info("Очистка корзины...");
            handleClearCart(chatId, callbackId);
        }
        else if (data.startsWith("cart_back")) {
            log.info("Возврат в корзину...");
            handleBackToCart(chatId);
        }
        else if (data.startsWith("cart_addon_syrup_")) {
            log.info("Обработка выбора сиропа...");
            handleCartAddSyrup(chatId, data);
        }
        else if (data.startsWith("cart_addon_milk_")) {
            log.info("Обработка выбора альтернативного молока...");
            handleCartAddMilk(chatId, data);
        } else if (data.startsWith("cart_delete_one")) {
            log.info("Обработка удаления конкретного товара");
            handleDeleteSomeProduct(chatId);
        }
    }

    /**
     * Возврат в корзину
     */
    private void handleBackToCart(Long chatId) {
        try {
            String cartDescription = cartService.getCartDescription(chatId);

            InlineKeyboardMarkup keyboard = keyboardService.createBasketKeyboard(chatId);

            SendMessage message = new SendMessage(chatId.toString(), cartDescription)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при возврате в корзину: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке корзины");
        }
    }

    /**
     * Очистка корзины
     */
    private void handleClearCart(Long chatId, String callbackId) {
        try {
            cartService.clearCart(chatId);

            messageSender.answerCallback(callbackId, "✅ Корзина очищена");

            String message = """
                🗑️ *Корзина очищена!*
                
                Ваша корзина была успешно очищена.
                """;

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton menuButton = new InlineKeyboardButton("🍽️ Перейти в меню")
                    .callbackData("category_null");
            keyboard.addRow(menuButton);

            SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(sendMessage);

        } catch (Exception e) {
            log.error("Ошибка очистки корзины: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при очистке корзины");
            messageSender.sendMessage(chatId, "❌ Ошибка при очистке корзины");
        }
    }

    /**
     * Обработка добавления в корзину
     */
    private void handleAddToCart(Long chatId, String callbackId, String data) {
        try {
            log.info("Добавление товара в корзину, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 4) {
                messageSender.answerCallback(callbackId, "❌ Ошибка в данных");
                return;
            }

            Long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            log.info("Товар ID: {}, количество: {}", productId, quantity);

            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            List<CartItem> addedItems = cartService.addProductToCart(chatId, productId, quantity);

            if (!addedItems.isEmpty()) {

                String message = getString(addedItems, product, quantity);

                method(chatId, message);

                messageSender.answerCallback(callbackId, "✅ Товар добавлен в корзину");
            } else {
                throw new Exception("Не удалось добавить товар в корзину");
            }

        } catch (Exception e) {
            log.error("Ошибка добавления в корзину: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при добавлении в корзину");
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении товара в корзину: " + e.getMessage());
        }
    }

    /**
     * Получение сложной строки
     */
    @NotNull
    private static String getString(List<CartItem> addedItems, Product product, int quantity) {
        CartItem firstItem = addedItems.getFirst();

        return String.format(
                """
                ✅ *Товар добавлен в корзину!*
                
                🛒 *%s* x%d
                💰 *Стоимость:* %d₽
                
                Товар успешно добавлен в вашу корзину.
                """,
                product.getName(),
                quantity,
                firstItem.calculateProductTotal()
        );
    }

    /**
     * Создание клавиатуры
     */
    private void method(Long chatId, String message) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton basketButton = new InlineKeyboardButton("🛒 Перейти в корзину")
                .callbackData("cart_back");
        keyboard.addRow(basketButton);

        InlineKeyboardButton continueButton = new InlineKeyboardButton("🛍️ Продолжить покупки")
                .callbackData("category_null");
        keyboard.addRow(continueButton);

        SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        bot.execute(sendMessage);
    }

    /**
     * Обработка добавления товара в корзину с добавкой
     */
    private void handleAddToCartWithAddon(Long chatId, String callbackId, String data) {
        try {
            log.info("Добавление товара с добавкой в корзину, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 6) {
                log.error("Недостаточно параметров: {}", data);
                messageSender.answerCallback(callbackId, "❌ Ошибка в данных");
                return;
            }

            Long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);
            Long addonProductId = Long.parseLong(parts[5]);
            int addonPrice = Integer.parseInt(parts[6]);

            log.info("Товар ID: {}, количество: {}, добавка ID: {}, цена добавки: {}",
                    productId, quantity, addonProductId, addonPrice);

            CartItem cartItem = cartService.addProductWithAddonToCart(
                    chatId, productId, quantity, addonProductId, addonPrice);

            if (cartItem != null) {
                Product product = cartItem.getProduct();
                Product addonProduct = productService.getProductById(addonProductId).orElse(null);

                String addonName = addonProduct != null ? addonProduct.getName() : "Добавка";

                String message = String.format(
                        """
                        ✅ *Товар с добавкой добавлен в корзину!*
                        
                        🛒 *%s* x%d
                        🍯 *Добавка:* %s (+%d₽ каждый)
                        💰 *Общая стоимость:* %d₽
                        
                        Товар успешно добавлен в вашу корзину.
                        """,
                        product.getName(),
                        quantity,
                        addonName,
                        addonPrice,
                        cartItem.calculateItemTotal()
                );

                method(chatId, message);
                messageSender.answerCallback(callbackId, "✅ Товар с добавкой добавлен в корзину");
            } else {
                throw new Exception("Не удалось добавить товар с добавкой в корзину");
            }

        } catch (Exception e) {
            log.error("Ошибка добавления в корзину с добавкой: {}", e.getMessage(), e);
            messageSender.answerCallback(callbackId, "❌ Ошибка при добавлении в корзину");
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении товара в корзину: " + e.getMessage());
        }
    }

    /**
     * Обработка добавления добавок к товарам в корзине
     */
    private void handleCartAddSyrup(Long chatId, String data) {
        try {
            String[] parts = data.split("_");
            long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);
            long categoryId = Long.parseLong(parts[5]);

            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            List<CartItem> cartItems = cartService.findCartItemByProduct(chatId, product.getName());

            if (cartItems == null || cartItems.isEmpty()) {
                messageSender.sendMessage(chatId, "🛒 Товар '" + product.getName() + "' не найден в вашей корзине");
                return;
            }

            String messageText = """
                🍯 *Добавление сиропа к товарам в корзине*
                
                Выберите товар, к которому хотите добавить добавки:
                """;

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            for (CartItem cartItem : cartItems) {
                product = cartItem.getProduct();

                if (keyboardService.needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_syrup_" + cartItem.getId() + "_" + productId + "_" + quantity + "_" + categoryId);
                    keyboard.addRow(itemButton);
                }
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору добавок")
                    .callbackData("addons_show_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при показе товаров для добавления добавок: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке товаров из корзины");
        }
    }

    /**
     * Обработка добавления добавок к товарам в корзине
     */
    private void handleCartAddMilk(Long chatId, String data) {
        try {
            String[] parts = data.split("_");
            long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);
            long categoryId = Long.parseLong(parts[5]);

            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            List<CartItem> cartItems = cartService.findCartItemByProduct(chatId, product.getName());

            if (cartItems == null || cartItems.isEmpty()) {
                messageSender.sendMessage(chatId, "🛒 Товар '" + product.getName() + "' не найден в вашей корзине");
                return;
            }

            String messageText = """
                🍯 *Добавление альтернативного молока к товарам в корзине*
                
                Выберите товар, к которому хотите добавить добавки:
                """;

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            for (CartItem cartItem : cartItems) {
                product = cartItem.getProduct();

                if (keyboardService.needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_milk_" + cartItem.getId() + "_" + productId + "_" + quantity + "_"  + categoryId);
                    keyboard.addRow(itemButton);
                }
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору добавок")
                    .callbackData("addons_show_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при показе товаров для добавления добавок: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке товаров из корзины");
        }
    }

    private void handleDeleteSomeProduct(Long chatId) {
        InlineKeyboardMarkup keyboard = keyboardService.createCartProductsKeyboard(chatId);
        String caption = "Выберите товар для удаления";
        SendMessage message = new SendMessage(chatId.toString(), caption)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard);

        bot.execute(message);
    }
}
