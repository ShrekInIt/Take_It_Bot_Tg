package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CallbackHandlerController {

    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CartService cartService;

    // Кэш для отслеживания типа последнего сообщения
    private final Map<Long, String> lastMessageType = new ConcurrentHashMap<>();

    // Метод для установки типа сообщения
    private void setLastMessageType(Long chatId, String type) {
        lastMessageType.put(chatId, type);
    }

    // Метод для получения предыдущего типа
    private String getAndUpdateLastMessageType(Long chatId, String newType) {
        String previous = lastMessageType.put(chatId, newType);
        log.info("Last message type for chat {}: previous={}, new={}", chatId, previous, newType);
        return previous;
    }

    /**
     * Основной обработчик callback-запросов от inline-кнопок
     */
    @SuppressWarnings("deprecation")
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        Message message = callbackQuery.message();
        if (message == null) {
            log.error("Message is null in callback query");
            return;
        }

        Long chatId = message.chat().id();
        String data = callbackQuery.data();
        Integer messageId = message.messageId();
        String callbackId = callbackQuery.id();

        log.info("=== ПОЛУЧЕН CALLBACK ===");
        log.info("Chat ID: {}, Message ID: {}, Data: {}", chatId, messageId, data);

        answerCallback(callbackId, null);
        System.out.println(data);
        try {
            if (data.startsWith("category_")) {
                log.info("Обработка категории...");
                handleCategoryCallback(chatId, messageId, data);
            } else if (data.startsWith("product_")) {
                log.info("Обработка товара...");
                handleProductCallback(chatId, data);
            } else if (data.startsWith("quantity_plus_") || data.startsWith("quantity_minus_")) {
                log.info("Обработка изменения количества...");
                handleQuantityChange(chatId, messageId, callbackId, data);
            } else if (data.startsWith("addons_")) {
                log.info("Обработка выбора добавок...");
                handleAddonsSelection(chatId, data);
            } else if (data.startsWith("add_to_cart_")) {
                log.info("Обработка добавления в корзину...");
                handleAddToCart(chatId, callbackId, data);
            } else if (data.startsWith("add_to_cart_with_addon_")) {
                log.info("Обработка добавления в корзину с добавкой...");
                handleAddToCartWithAddon(chatId, callbackId, data);
            } else if (data.startsWith("quantity_display_")) {
                log.info("Нажата кнопка отображения количества (игнорируем)...");
                answerCallback(callbackId, "✏️ Измените количество кнопками ➖ и ➕");
            } else if (data.startsWith("addon_syrup_")) {
                log.info("Обработка выбора сиропа...");
                handleSyrupSelection(chatId, data);
            } else if (data.startsWith("addon_milk_")) {
                log.info("Обработка выбора альтернативного молока...");
                handleMilkSelection(chatId, data);
            } else if (data.startsWith("choose_addon_")) {
                log.info("Обработка выбора конкретной добавки...");
                handleAddonSelection(chatId, data);
            } else if (data.startsWith("clear_cart")) {
                log.info("Очистка корзины...");
                handleClearCart(chatId, callbackId);
            } else if (data.startsWith("create_order")) {
                log.info("Создание заказа...");
                handleCreateOrder(chatId, callbackId);
            } else if (data.startsWith("back_to_cart")) {
                log.info("Возврат в корзину...");
                handleBackToCart(chatId);
            } else {
                log.warn("Неизвестный callback: {}", data);
                answerCallback(callbackId, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка обработки запроса");
        }
    }

    /**
     * Обработка выбора добавок (только для кофе)
     */
    private void handleAddonsSelection(Long chatId, String data) {
        try {
            log.info("Обработка выбора добавок, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 3) {
                log.error("Недостаточно параметров: {}", data);
                sendMessage(chatId, "❌ Ошибка в данных добавок");
                return;
            }

            Long productId = Long.parseLong(parts[1]);
            int quantity = Integer.parseInt(parts[2]);

            log.info("Товар ID: {}, количество: {}", productId, quantity);

            productService.getProductById(productId).ifPresent(
                    product -> {
                        String messageText = String.format(
                                """
                                🍯 *Выбор добавок для:* %s
                                
                                Количество: %d шт.
                                
                                Выберите тип добавки:""",
                                product.getName(),
                                quantity
                        );

                        InlineKeyboardMarkup keyboard = createAddonsKeyboard(productId, quantity);

                        SendMessage sendMessage = new SendMessage(chatId.toString(), messageText)
                                .parseMode(ParseMode.Markdown)
                                .replyMarkup(keyboard);

                        bot.execute(sendMessage);
                    }
            );

        } catch (Exception e) {
            log.error("Ошибка выбора добавок: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при выборе добавок");
        }
    }

    /**
     * Создать клавиатуру для выбора добавок
     */
    private InlineKeyboardMarkup createAddonsKeyboard(Long productId, int quantity) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        // ID категорий из таблицы категорий
        Long syrupCategoryId = 20L; // Сиропы
        Long milkCategoryId = 21L;  // Альтернативное молоко

        // Проверяем наличие сиропов (из таблицы продуктов)
        List<Product> syrups = productService.getAvailableProductsWithStock(syrupCategoryId);
        log.info("Найдено сиропов: {}", syrups.size());

        if (!syrups.isEmpty()) {
            InlineKeyboardButton syrupButton = new InlineKeyboardButton("🍯 Сиропы")
                    .callbackData("addon_syrup_" + productId + "_" + quantity);
            keyboard.addRow(syrupButton);
        }

        // Проверяем наличие молока (из таблицы продуктов)
        List<Product> milks = productService.getAvailableProductsWithStock(milkCategoryId);
        log.info("Найдено видов молока: {}", milks.size());

        if (!milks.isEmpty()) {
            InlineKeyboardButton milkButton = new InlineKeyboardButton("🥛 Альтернативное молоко")
                    .callbackData("addon_milk_" + productId + "_" + quantity);
            keyboard.addRow(milkButton);
        }

        // Если нет доступных добавок
        if (keyboard.inlineKeyboard() == null || keyboard.inlineKeyboard().length == 0) {
            InlineKeyboardButton noAddonsButton = new InlineKeyboardButton("⚠️ Добавки временно недоступны")
                    .callbackData("no_action");
            keyboard.addRow(noAddonsButton);
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к товару")
                .callbackData("product_" + productId);
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Обработка выбора сиропов
     */
    private void handleSyrupSelection(Long chatId, String data) {
        try {
            log.info("Обработка выбора сиропа, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 4) {
                sendMessage(chatId, "❌ Ошибка в данных сиропа");
                return;
            }

            Long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            // Получаем доступные сиропы (категория 20)
            List<Product> syrups = productService.getAvailableProductsWithStock(20L);

            log.info("Доступно сиропов: {}", syrups.size());

            if (syrups.isEmpty()) {
                sendMessage(chatId, "❌ В данный момент сиропы недоступны");
                return;
            }

            productService.getProductById(productId).ifPresent(
                    product -> {
                        String messageText = String.format(
                                """
                                🍯 *Выбор сиропа для:* %s
                                
                                Количество: %d шт.
                                
                                Выберите сироп:""",
                                product.getName(),
                                quantity
                        );

                        InlineKeyboardMarkup keyboard = createSyrupsKeyboard(productId, quantity, syrups);

                        SendMessage sendMessage = new SendMessage(chatId.toString(), messageText)
                                .parseMode(ParseMode.Markdown)
                                .replyMarkup(keyboard);

                        bot.execute(sendMessage);
                    }
            );

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при выборе сиропа");
        }
    }

    /**
     * Обработка выбора альтернативного молока
     */
    private void handleMilkSelection(Long chatId, String data) {
        try {
            log.info("Обработка выбора молока, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 4) {
                sendMessage(chatId, "❌ Ошибка в данных молока");
                return;
            }

            Long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            // Получаем доступное альтернативное молоко (категория 21)
            List<Product> milks = productService.getAvailableProductsWithStock(21L);

            log.info("Доступно видов молока: {}", milks.size());

            if (milks.isEmpty()) {
                sendMessage(chatId, "❌ В данный момент альтернативное молоко недоступно");
                return;
            }

            productService.getProductById(productId).ifPresent(
                    product -> {
                        String messageText = String.format(
                                """
                                🥛 *Выбор альтернативного молока для:* %s
                                
                                Количество: %d шт.
                                
                                Выберите молоко:""",
                                product.getName(),
                                quantity
                        );

                        InlineKeyboardMarkup keyboard = createMilksKeyboard(productId, quantity, milks);

                        SendMessage sendMessage = new SendMessage(chatId.toString(), messageText)
                                .parseMode(ParseMode.Markdown)
                                .replyMarkup(keyboard);

                        bot.execute(sendMessage);
                    }
            );

        } catch (Exception e) {
            log.error("Ошибка выбора альтернативного молока: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при выборе альтернативного молока");
        }
    }

    /**
            * Создать клавиатуру с доступными сиропами
    */
    private InlineKeyboardMarkup createSyrupsKeyboard(Long productId, int quantity, List<Product> syrups) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Product syrup : syrups) {
            String buttonText = String.format("%s (+%d₽)", syrup.getName(), syrup.getAmount());
            InlineKeyboardButton syrupButton = new InlineKeyboardButton(buttonText)
                    .callbackData("choose_addon_" + productId + "_" + syrup.getId() + "_" + quantity + "_" + syrup.getAmount());
            keyboard.addRow(syrupButton);
        }

        // Кнопка "Назад" к выбору типа добавок
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("addons_" + productId + "_" + quantity);
        keyboard.addRow(backButton);

        return keyboard;
    }


    /**
     * Создать клавиатуру с доступным альтернативным молоком
     */
    private InlineKeyboardMarkup createMilksKeyboard(Long productId, int quantity, List<Product> milks) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Product milk : milks) {
            String buttonText = String.format("%s (+%d₽)", milk.getName(), milk.getAmount());
            InlineKeyboardButton milkButton = new InlineKeyboardButton(buttonText)
                    .callbackData("choose_addon_" + productId + "_" + milk.getId() + "_" + quantity + "_" + milk.getAmount());
            keyboard.addRow(milkButton);
        }

        // Кнопка "Назад" к выбору типа добавок
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("addons_" + productId + "_" + quantity);
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Обработка выбора конкретной добавки
     */
    private void handleAddonSelection(Long chatId, String data) {
        try {
            log.info("Обработка выбора добавки, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 6) {
                sendMessage(chatId, "❌ Ошибка в данных добавки");
                return;
            }

            Long productId = Long.parseLong(parts[2]);
            Long addonProductId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);
            int addonPrice = Integer.parseInt(parts[5]);

            // Получаем информацию о продукте и добавке
            var productOpt = productService.getProductById(productId);
            var addonOpt = productService.getProductById(addonProductId);

            if (productOpt.isPresent() && addonOpt.isPresent()) {
                Product product = productOpt.get();
                Product addon = addonOpt.get();

                int totalPrice = (product.getAmount() * quantity) + (addonPrice * quantity);

                String messageText = String.format(
                        """
                        ✅ *Добавка выбрана!*
                        
                        *Основной продукт:* %s
                        *Добавка:* %s
                        *Доплата:* +%d₽
                        *Количество:* %d шт.
                        
                        *Общая стоимость:* %d₽""",
                        product.getName(),
                        addon.getName(),
                        addonPrice,
                        quantity,
                        totalPrice
                );

                // Создаем клавиатуру
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Добавить в корзину")
                        .callbackData("add_to_cart_with_addon_" + productId + "_" + quantity + "_" + addonProductId + "_" + addonPrice);
                keyboard.addRow(confirmButton);

                // Кнопка "Назад" в зависимости от категории добавки
                Long addonCategoryId = addon.getCategoryId();
                String backCallback;

                if (addonCategoryId.equals(20L)) {
                    backCallback = "addon_syrup_" + productId + "_" + quantity;
                } else if (addonCategoryId.equals(21L)) {
                    backCallback = "addon_milk_" + productId + "_" + quantity;
                } else {
                    backCallback = "addons_" + productId + "_" + quantity;
                }

                InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                        .callbackData(backCallback);
                keyboard.addRow(backButton);

                SendMessage sendMessage = new SendMessage(chatId.toString(), messageText)
                        .parseMode(ParseMode.Markdown)
                        .replyMarkup(keyboard);
                bot.execute(sendMessage);

            } else {
                log.error("Продукт или добавка не найдены");
                sendMessage(chatId, "❌ Продукт или добавка не найдены");
            }

        } catch (Exception e) {
            log.error("Ошибка выбора добавки: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при выборе добавки");
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
                log.error("Недостаточно параметров: {}", data);
                answerCallback(callbackId, "❌ Ошибка в данных");
                return;
            }

            Long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);

            log.info("Товар ID: {}, количество: {}", productId, quantity);

            // Добавляем товар в корзину
            CartItem cartItem = cartService.addProductToCart(chatId, productId, quantity);

            if (cartItem != null) {
                Product product = cartItem.getProduct();
                String message = String.format(
                        """
                        ✅ *Товар добавлен в корзину!*
                        
                        🛒 *%s* x%d
                        💰 *Стоимость:* %d₽
                        
                        Товар успешно добавлен в вашу корзину.
                        """,
                        product.getName(),
                        quantity,
                        cartItem.calculateProductTotal()
                );

                // Создаем клавиатуру с действиями
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                InlineKeyboardButton basketButton = new InlineKeyboardButton("🛒 Перейти в корзину")
                        .callbackData("back_to_cart");
                keyboard.addRow(basketButton);

                InlineKeyboardButton continueButton = new InlineKeyboardButton("🛍️ Продолжить покупки")
                        .callbackData("category_null");
                keyboard.addRow(continueButton);

                SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                        .parseMode(ParseMode.Markdown)
                        .replyMarkup(keyboard);

                bot.execute(sendMessage);
                answerCallback(callbackId, "✅ Товар добавлен в корзину");
            } else {
                throw new Exception("Не удалось добавить товар в корзину");
            }

        } catch (Exception e) {
            log.error("Ошибка добавления в корзину: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка при добавлении в корзину");
            sendMessage(chatId, "❌ Ошибка при добавлении товара в корзину: " + e.getMessage());
        }
    }

    /**
     * Обработка добавления товара в корзину с добавкой
     */
    private void handleAddToCartWithAddon(Long chatId, String callbackId, String data) {
        try {
            log.info("Добавление товара с добавкой в корзину, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 7) {
                log.error("Недостаточно параметров: {}", data);
                answerCallback(callbackId, "❌ Ошибка в данных");
                return;
            }

            Long productId = Long.parseLong(parts[4]);
            int quantity = Integer.parseInt(parts[5]);
            Long addonProductId = Long.parseLong(parts[6]);
            int addonPrice = Integer.parseInt(parts[7]);

            log.info("Товар ID: {}, количество: {}, добавка ID: {}, цена добавки: {}",
                    productId, quantity, addonProductId, addonPrice);

            // Добавляем товар с добавкой в корзину
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

                // Создаем клавиатуру с действиями
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                InlineKeyboardButton basketButton = new InlineKeyboardButton("🛒 Перейти в корзину")
                        .callbackData("back_to_cart");
                keyboard.addRow(basketButton);

                InlineKeyboardButton continueButton = new InlineKeyboardButton("🛍️ Продолжить покупки")
                        .callbackData("category_null");
                keyboard.addRow(continueButton);

                SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                        .parseMode(ParseMode.Markdown)
                        .replyMarkup(keyboard);

                bot.execute(sendMessage);
                answerCallback(callbackId, "✅ Товар с добавкой добавлен в корзину");
            } else {
                throw new Exception("Не удалось добавить товар с добавкой в корзину");
            }

        } catch (Exception e) {
            log.error("Ошибка добавления в корзину с добавкой: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка при добавлении в корзину");
            sendMessage(chatId, "❌ Ошибка при добавлении товара в корзину: " + e.getMessage());
        }
    }

    /**
     * Очистка корзины
     */
    private void handleClearCart(Long chatId, String callbackId) {
        try {
            cartService.clearCart(chatId);

            answerCallback(callbackId, "✅ Корзина очищена");

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
            answerCallback(callbackId, "❌ Ошибка при очистке корзины");
            sendMessage(chatId, "❌ Ошибка при очистке корзины");
        }
    }

    /**
     * Создание заказа
     */
    private void handleCreateOrder(Long chatId, String callbackId) {
        try {
            // Проверяем, не пуста ли корзина
            if (cartService.isCartEmpty(chatId)) {
                answerCallback(callbackId, "❌ Корзина пуста");
                sendMessage(chatId, "❌ Ваша корзина пуста. Добавьте товары перед оформлением заказа.");
                return;
            }

            // Получаем общую сумму
            int totalAmount = cartService.getCartTotal(chatId);

            String message = String.format(
                    """
                    📝 *Оформление заказа*
                    
                    💰 *Сумма заказа:* %d₽
                    
                    Пожалуйста, выберите способ получения:
                    """,
                    totalAmount
            );

            // Создаем клавиатуру с вариантами получения
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton pickupButton = new InlineKeyboardButton("🚶 Самовывоз")
                    .callbackData("order_pickup");
            keyboard.addRow(pickupButton);

            InlineKeyboardButton deliveryButton = new InlineKeyboardButton("🚚 Доставка")
                    .callbackData("order_delivery");
            keyboard.addRow(deliveryButton);

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад в корзину")
                    .callbackData("back_to_cart");
            keyboard.addRow(backButton);

            SendMessage sendMessage = new SendMessage(chatId.toString(), message)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(sendMessage);
            answerCallback(callbackId, "Выберите способ получения");

        } catch (Exception e) {
            log.error("Ошибка создания заказа: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка при оформлении заказа");
            sendMessage(chatId, "❌ Ошибка при оформлении заказа");
        }
    }

    /**
     * Возврат в корзину
     */
    private void handleBackToCart(Long chatId) {
        try {
            // Получаем содержимое корзины
            String cartDescription = cartService.getCartDescription(chatId);

            // Создаем клавиатуру для корзины
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            if (cartService.isCartEmpty(chatId)) {
                InlineKeyboardButton menuButton = new InlineKeyboardButton("🍽️ Перейти в меню")
                        .callbackData("category_null");
                keyboard.addRow(menuButton);
            } else {
                InlineKeyboardButton clearButton = new InlineKeyboardButton("🗑️ Очистить корзину")
                        .callbackData("clear_cart");
                InlineKeyboardButton orderButton = new InlineKeyboardButton("📝 Оформить заказ")
                        .callbackData("create_order");
                keyboard.addRow(clearButton, orderButton);

                InlineKeyboardButton continueShoppingButton = new InlineKeyboardButton("🛒 Продолжить покупки")
                        .callbackData("category_null");
                keyboard.addRow(continueShoppingButton);
            }

            SendMessage message = new SendMessage(chatId.toString(), cartDescription)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при возврате в корзину: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при загрузке корзины");
        }
    }


    /**
     * Обработка callback-запросов для категорий
     */
    private void handleCategoryCallback(Long chatId, Integer messageId, String data) {

        String categoryIdStr = data.substring("category_".length());
        System.out.println(categoryIdStr);
        if ("null".equals(categoryIdStr)) {
            showRootCategories(chatId, messageId);
            return;
        }

        try {
            Long categoryId = Long.parseLong(categoryIdStr);
            showCategory(chatId, messageId, categoryId);
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Ошибка в данных категории");
        }
    }


    /**
     * Обработка callback-запросов для товаров
     */
    private void handleProductCallback(Long chatId, String data) {
        String productPart = data.substring("product_".length());
        String[] parts = productPart.split("_");

        try {
            Long productId = Long.parseLong(parts[0]);
            Long sourceCategoryId = null;

            // Если есть второй параметр - это sourceCategoryId
            if (parts.length > 1 && !parts[1].isEmpty()) {
                sourceCategoryId = Long.parseLong(parts[1]);
            }

            log.info("Пользователь выбрал товар ID: {}, из категории: {}", productId, sourceCategoryId);
            showProductDetails(chatId, productId, sourceCategoryId);
        } catch (NumberFormatException e) {
            log.error("Ошибка в данных товара: {}", data);
            sendMessage(chatId, "❌ Ошибка в данных товара");
        }
    }

    /**
     * Показать детали товара с фото и кнопками
     */
    private void showProductDetails(Long chatId, Long productId, Long sourceCategoryId) {
        productService.getProductById(productId).ifPresentOrElse(
                product -> {

                    setLastMessageType(chatId, "product");
                    String caption = getString(product.getAmount(), product, 1);

                    // 2. Проверяем, нужны ли добавки
                    if (keyboardService.needsAddons(product)) {
                        caption += "\n\n<i>К этому напитку можно добавить сиропы или альтернативное молоко</i>";
                    }

                    // 3. Создаем клавиатуру (только кнопки!)
                    InlineKeyboardMarkup keyboard = keyboardService.createProductKeyboard(product, 1, sourceCategoryId);

                    // 4. Пытаемся отправить ФОТО с подписью и клавиатурой
                    if (product.getPhoto() != null && !product.getPhoto().isEmpty()) {
                        try {
                            // Читаем фото
                            byte[] photoBytes = keyboardService.readPhotoFile(product.getPhoto());

                            if (photoBytes != null && photoBytes.length > 0) {
                                // Отправляем фото с подписью и клавиатурой
                                SendPhoto sendPhoto = new SendPhoto(chatId.toString(), photoBytes)
                                        .caption(caption)
                                        .parseMode(ParseMode.HTML)
                                        .replyMarkup(keyboard); // <- добавляем клавиатуру к фото

                                bot.execute(sendPhoto);
                                return;
                            }
                        } catch (Exception e) {
                            log.error("Ошибка отправки фото товара: {}", e.getMessage());
                        }
                    }

                    // 5. Если фото нет или не удалось отправить, отправляем ТЕКСТ с клавиатурой
                    SendMessage message = new SendMessage(chatId.toString(), caption)
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(keyboard); // <- добавляем клавиатуру к тексту

                    bot.execute(message);
                },
                () -> sendMessage(chatId, "❌ Товар не найден")
        );
    }

    /**
     * Обработка изменения количества товара
     */
    private void handleQuantityChange(Long chatId, Integer messageId, String callbackId, String data) {
        try {
            log.info("=== ОБРАБОТКА ИЗМЕНЕНИЯ КОЛИЧЕСТВА ===");
            log.info("Callback data: {}", data);
            log.info("Chat ID: {}, Message ID: {}", chatId, messageId);

            String[] parts = data.split("_");

            if (parts.length < 6) {
                log.error("Некорректный формат данных: {}", data);
                return;
            }

            String action = parts[1]; // "plus" или "minus"
            Long productId = Long.parseLong(parts[2]);
            int currentQuantity = Integer.parseInt(parts[3]);
            Long sourceCategoryId = "null".equals(parts[4]) ? null : Long.parseLong(parts[4]);

            log.info("Действие: {}, Товар ID: {}, Текущее кол-во: {}", action, productId, currentQuantity);

            // Получаем продукт
            var productOpt = productService.getProductById(productId);
            if (productOpt.isEmpty()) {
                log.error("Товар с ID {} не найден", productId);
                answerCallback(callbackId, "❌ Товар не найден");
                return;
            }

            Product product = productOpt.get();
            int newQuantity = currentQuantity;

            if ("plus".equals(action)) {
                // Проверяем доступное количество
                if (product.getCount() != null && currentQuantity >= product.getCount()) {
                    String message = "⛔ В наличии только " + product.getCount() + " шт.";
                    log.warn(message);
                    answerCallback(callbackId, message);
                    return;
                }
                newQuantity = Math.min(currentQuantity + 1, 99); // Максимум 99
                log.info("Увеличиваем количество с {} до {}", currentQuantity, newQuantity);
            } else if ("minus".equals(action)) {
                newQuantity = Math.max(currentQuantity - 1, 1); // Минимум 1
                log.info("Уменьшаем количество с {} до {}", currentQuantity, newQuantity);
            }

            // Если количество не изменилось, не обновляем сообщение
            if (newQuantity == currentQuantity) {
                log.info("Количество не изменилось, пропускаем обновление");
                return;
            }

            // Обновляем сообщение с новым количеством
            updateProductMessageWithSource(chatId, messageId, productId, newQuantity, sourceCategoryId);

            log.info("✅ Сообщение обновлено с новым количеством: {}", newQuantity);

        } catch (Exception e) {
            log.error("Ошибка обработки изменения количества: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка изменения количества");
        }
    }

    /**
     * Обновить сообщение с товаром с новым количеством и sourceCategoryId
     */
    private void updateProductMessageWithSource(Long chatId, Integer messageId, Long productId,
                                                int quantity, Long sourceCategoryId) {
        try {
            productService.getProductById(productId).ifPresent(product -> {
                String caption = getString(product.getAmount() * quantity, product, quantity);

                if (keyboardService.needsAddons(product)) {
                    caption += "\n\n<i>К этому напитку можно добавить сиропы или альтернативное молоко</i>";
                }

                InlineKeyboardMarkup keyboard = keyboardService.createProductKeyboard(product, quantity, sourceCategoryId);

                // Для обновления товара (фото->фото или текст->текст) используем прямое редактирование
                boolean editSuccess = false;

                try {
                    com.pengrad.telegrambot.request.EditMessageCaption editCaption =
                            new com.pengrad.telegrambot.request.EditMessageCaption(chatId, messageId)
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

                // Если редактирование не удалось, ТОЛЬКО ТОГДА заменяем
                if (!editSuccess) {
                    log.warn("Не удалось отредактировать товар, заменяем сообщение");
                    replaceMessage(chatId, messageId, caption, keyboard);
                }
            });
        } catch (Exception e) {
            log.error("Ошибка обновления товара: {}", e.getMessage());
        }
    }

    /**
     * Обновить сообщение с товаром с новым количеством
     */
    private void updateProductMessage(Long chatId, Integer messageId, Long productId, int quantity) {
        try {
            productService.getProductById(productId).ifPresent(product -> {
                String caption = getString(product.getAmount() * quantity, product, quantity);

                if (keyboardService.needsAddons(product)) {
                    caption += "\n\n<i>К этому напитку можно добавить сиропы или альтернативное молоко</i>";
                }

                // ВАЖНО: Нужно получить sourceCategoryId из текущего сообщения
                // Проблема: мы не знаем sourceCategoryId в этом методе

                // Временное решение: используем null, но это не исправит навигацию
                Long sourceCategoryId = null; // ← ЭТО ВРЕМЕННО!

                // Правильное решение: передавать sourceCategoryId через callback данных
                InlineKeyboardMarkup keyboard = keyboardService.createProductKeyboard(product, quantity, sourceCategoryId);

                // Пробуем отредактировать
                boolean editSuccess = false;

                try {
                    com.pengrad.telegrambot.request.EditMessageCaption editCaption =
                            new com.pengrad.telegrambot.request.EditMessageCaption(chatId, messageId)
                                    .caption(caption)
                                    .parseMode(ParseMode.HTML)
                                    .replyMarkup(keyboard);

                    bot.execute(editCaption);
                    editSuccess = true;
                } catch (Exception e1) {
                    log.debug("Не удалось отредактировать caption, пробуем текст");
                }

                if (!editSuccess) {
                    try {
                        EditMessageText editText = new EditMessageText(chatId, messageId, caption)
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(keyboard);

                        bot.execute(editText);
                        editSuccess = true;
                    } catch (Exception e2) {
                        log.error("Не удалось отредактировать текст");
                    }
                }

                // Если редактирование не удалось, заменяем сообщение
                if (!editSuccess) {
                    replaceMessage(chatId, messageId, caption, keyboard);
                }
            });
        } catch (Exception e) {
            log.error("Ошибка обновления товара: {}", e.getMessage());
        }
    }

    /**
     * Возвращение сложной строки для описания продукта
     */
    @NotNull
    private static String getString(int product, Product product1, int quantity) {
        return String.format(
                "<b>%s</b>\n%s\n\n<b>Цена:</b> %d₽\n<b>Размер:</b> %s\n<b>Количество:</b> %d шт.\n<b>Итого:</b> <b>%d₽</b>",
                product1.getName(),
                product1.getDescription() != null ? "\n" + product1.getDescription() + "\n" : "",
                product1.getAmount(),
                product1.getSize() != null ? product1.getSize() : "Стандартный",
                quantity,
                product
        );
    }

    /**
     * Показать корневые категории (главное меню категорий)
     */
    private void showRootCategories(Long chatId, Integer messageId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            sendMessage(chatId, "📂 Категории пока не добавлены");
            return;
        }

        EditMessageText editMessage = new EditMessageText(chatId, messageId, "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeEditMessage(chatId, editMessage);
    }

    /**
     * Показать содержимое категории: подкатегории и/или товары
     */
    @Transactional(readOnly = true) // Добавьте эту аннотацию
    protected void showCategory(Long chatId, Integer messageId, Long categoryId) {
        log.info("showCategory: chatId={}, messageId={}, categoryId={}", chatId, messageId, categoryId);

        Category category = categoryService.getCategoryWithParent(categoryId);
        if (category == null) {
            log.warn("Category not found for id: {}", categoryId);
            sendMessage(chatId, "❌ Категория не найдена");
            return;
        }

        log.info("Found category: name={}, active={}", category.getName(), category.isActive());

        if (!category.isActive()) {
            sendMessage(chatId, "📭 Эта категория временно недоступна");
            return;
        }

        List<Category> subcategories = categoryService.getActiveSubcategories(categoryId);
        boolean hasSubcategories = !subcategories.isEmpty();
        boolean hasProducts = productService.hasAvailableProductsInCategory(categoryId);

        // Определяем, пришли ли мы из товара
        String previousType = getAndUpdateLastMessageType(chatId, "category");
        boolean fromProduct = "product".equals(previousType);

        log.info("Subcategories found: {}, Products available: {}", hasSubcategories, hasProducts);

        if (hasSubcategories && hasProducts) {
            log.info("Path: Has both subcategories and products");
            // Есть и подкатегории (после фильтрации) и товары
            List<Product> products = productService.getAvailableProductsWithStock(categoryId);
            InlineKeyboardMarkup keyboard = categoryService.createCombinedKeyboard(categoryId, subcategories, products);
            String messageText = categoryService.getCategoryDescription(category, true, true);

            smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else if (hasSubcategories) {
            log.info("Path: Has only subcategories");
            // Только подкатегории
            InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(categoryId);
            String messageText = categoryService.getCategoryDescription(category, true, false);

            smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else if (hasProducts) {
            log.info("Path: Has only products");
            // Только товары
            InlineKeyboardMarkup keyboard = keyboardService.getProductsWithQuantityKeyboard(categoryId);
            String messageText = categoryService.getCategoryDescription(category, false, true);

            smartUpdateMessage(chatId, messageId, messageText, keyboard, fromProduct);

        } else {
            log.warn("Path: No subcategories or products found!");
            // Ничего нет
            sendMessage(chatId, "📭 В этой категории пока нет доступных товаров");
        }
    }

    /**
     * Умное обновление сообщения: пробуем редактировать, учитывая тип сообщения
     * @param fromProduct true, если переходим из товара в категорию
     */
    private void smartUpdateMessage(Long chatId, Integer messageId, String newText,
                                    InlineKeyboardMarkup keyboard, boolean fromProduct) {
        try {
            // Если переходим из товара (фото) в категорию (текст),
            // редактирование может не работать
            if (fromProduct) {
                log.info("Переход из товара в категорию, используем осторожное обновление");
                // Пробуем отредактировать, но если не получится - заменяем
                try {
                    EditMessageText editMessage = new EditMessageText(chatId, messageId, newText)
                            .parseMode(ParseMode.Markdown)
                            .replyMarkup(keyboard);

                    var response = bot.execute(editMessage);

                    if (response.isOk()) {
                        log.info("Успешно отредактировали фото->текст");
                        return;
                    } else {
                        log.warn("EditMessage не удалось при переходе из товара, заменяем");
                        replaceMessage(chatId, messageId, newText, keyboard);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при редактировании фото->текст: {}", e.getMessage());
                    replaceMessage(chatId, messageId, newText, keyboard);
                }
            } else {
                // Переход между категориями (текст->текст) - пробуем редактировать
                EditMessageText editMessage = new EditMessageText(chatId, messageId, newText)
                        .parseMode(ParseMode.Markdown)
                        .replyMarkup(keyboard);

                var response = bot.execute(editMessage);

                if (response.isOk()) {
                    log.info("Сообщение успешно отредактировано (текст->текст)");
                } else {
                    log.warn("EditMessage не удалось (текст->текст), заменяем сообщение");
                    replaceMessage(chatId, messageId, newText, keyboard);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщения: {}", e.getMessage());
            // Последняя попытка - заменить сообщение
            replaceMessage(chatId, messageId, newText, keyboard);
        }
    }

    /**
     * Удалить сообщение и отправить новое вместо редактирования
     */
    private void replaceMessage(Long chatId, Integer messageIdToDelete, String newText,
                                InlineKeyboardMarkup keyboard) {
        try {
            // 1. Отправляем новое сообщение
            SendMessage newMessage = new SendMessage(chatId.toString(), newText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(newMessage);
            log.info("Новое сообщение отправлено");

            // 2. Пытаемся удалить старое сообщение (в фоновом режиме, не блокируем)
            if (messageIdToDelete != null) {
                try {
                    // Небольшая задержка для плавности
                    new Thread(() -> {
                        try {
                            Thread.sleep(300); // 300ms задержка
                            com.pengrad.telegrambot.request.DeleteMessage deleteMsg =
                                    new com.pengrad.telegrambot.request.DeleteMessage(chatId, messageIdToDelete);
                            bot.execute(deleteMsg);
                            log.info("Старое сообщение {} удалено", messageIdToDelete);
                        } catch (Exception e) {
                            // Не критично, если не удалось удалить
                            log.debug("Не удалось удалить старое сообщение: {}", e.getMessage());
                        }
                    }).start();
                } catch (Exception e) {
                    log.debug("Ошибка при планировании удаления: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка замены сообщения: {}", e.getMessage());
            sendMessage(chatId, "❌ Ошибка обновления");
        }
    }

    /**
     * Выполнить редактирование сообщения с новой клавиатурой
     */
    private void executeEditMessage(Long chatId, EditMessageText editMessage) {
        try {
            var response = bot.execute(editMessage);
            log.info("EditMessage successful. Response: {}", response);
            bot.execute(editMessage);
        } catch (Exception e) {
            log.error("Error editing message for chat {}: {}", chatId, e.getMessage(), e);
            log.error("Error editing message for chat {}: {}", chatId, e.getMessage());
            sendMessage(chatId, "❌ Ошибка при обновлении сообщения");
        }
    }

    /**
     * Отправить текстовое сообщение пользователю
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.Markdown);

        try {
            bot.execute(message);
        } catch (Exception e) {
            log.error("Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Ответить на callback-запрос (подтвердить получение)
     */
    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);

        if (text != null && !text.isEmpty()) {
            answer.text(text).showAlert(true);
        } else {
            answer.text("").showAlert(false);
        }

        try {
            bot.execute(answer);
        } catch (Exception e) {
            log.error("Error answering callback: {}", e.getMessage());
        }
    }
}
