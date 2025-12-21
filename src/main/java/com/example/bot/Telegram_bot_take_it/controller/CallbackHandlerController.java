package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.*;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

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
    private final CartItemService cartItemService;
    private final CartItemAddonService cartItemAddonService;

    // Кэш для отслеживания типа последнего сообщения
    private final Map<Long, String> lastMessageType = new ConcurrentHashMap<>();

    /**
     * Метод для установки типа сообщения
     */
    private void setLastMessageType(Long chatId) {
        lastMessageType.put(chatId, "product");
    }

    // Метод для получения предыдущего типа
    private String getAndUpdateLastMessageType(Long chatId) {
        String previous = lastMessageType.put(chatId, "category");
        log.info("Last message type for chat {}: previous={}, new={}", chatId, previous, "category");
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
            }
            else if (data.startsWith("product_")) {
                log.info("Обработка товара...");
                handleProductCallback(chatId, data);
            }
            else if (data.startsWith("quantity_plus_") || data.startsWith("quantity_minus_")) {
                log.info("Обработка изменения количества...");
                handleQuantityChange(chatId, messageId, callbackId, data);
            }
            else if (data.startsWith("addons_")) {
                log.info("Обработка выбора добавок...");
                handleAddonsSelection(chatId, data);
            }
            else if (data.startsWith("milk_addons_")) {
                log.info("Обработка выбора добавок...");
                handleAddonsSelectionMilk(chatId, data);
            }
            else if (data.startsWith("syrup_addons_")) {
                log.info("Обработка выбора добавок...");
                handleAddonsSelectionSyrup(chatId, data);
            }
            else if (data.startsWith("add_syrup_")) {
                showSyrupsSelection(chatId, data);
            }
            else if (data.startsWith("add_milk_")) {
                showMilksSelection(chatId, data);
            }
            else if (data.startsWith("remove_syrup_")) {
                removeSelectedSyrup(chatId, messageId, data);
            }
            else if (data.startsWith("remove_milk_")) {
                removeSelectedMilk(chatId, messageId, data);
            }
            else if (data.startsWith("select_milk_")) {
                handleSelectMilk(chatId, data);
            }
            else if (data.startsWith("select_syrup_")) {
                handleSelectSyrup(chatId, data);
            }
            else if (data.startsWith("add_to_cart_")) {
                log.info("Обработка добавления в корзину...");
                handleAddToCart(chatId, callbackId, data);
            }
            else if (data.startsWith("add_to_cart_with_addon_")) {
                log.info("Обработка добавления в корзину с добавкой...");
                handleAddToCartWithAddon(chatId, callbackId, data);
            }
            else if (data.startsWith("quantity_display_")) {
                log.info("Нажата кнопка отображения количества (игнорируем)...");
                answerCallback(callbackId, "✏️ Измените количество кнопками ➖ и ➕");
            }
            else if (data.startsWith("addon_syrup_")) {
                log.info("Обработка выбора сиропа...");
                handleCartAddSyrup(chatId, data);
            }
            else if (data.startsWith("addon_milk_")) {
                log.info("Обработка выбора альтернативного молока...");
                handleCartAddMilk(chatId, data);
            }
            else if (data.startsWith("choose_addon_")) {
                log.info("Обработка выбора конкретной добавки...");
                handleAddonSelection(chatId, data);
            }
            else if (data.startsWith("clear_cart")) {
                log.info("Очистка корзины...");
                handleClearCart(chatId, callbackId);
            }
            else if (data.startsWith("create_order")) {
                log.info("Создание заказа...");
                handleCreateOrder(chatId, callbackId);
            }
            else if (data.startsWith("back_to_cart")) {
                log.info("Возврат в корзину...");
                handleBackToCart(chatId);
            }
            else {
                log.warn("Неизвестный callback: {}", data);
                answerCallback(callbackId, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage(), e);
            answerCallback(callbackId, "❌ Ошибка обработки запроса");
        }
    }

    private void removeSelectedMilk(Long chatId, Integer messageId, String data) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.parseLong(parts[2]);

            Product oldMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);
            if (oldMilk != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldMilk.getId());
            }

            // Обновляем сообщение с добавками вместо отправки нового
            updateAddonsMessage(chatId, messageId, data, "milk");

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    private void handleSelectMilk(Long chatId, String data) {
        try {
            // Пример данных: "select_syrup_22_45" (cartItemId=22, syrupId=45)
            String[] parts = data.split("_");
            Long cartItemId = Long.valueOf(parts[2]);
            Long milkId = Long.valueOf(parts[3]);
            long productId = Long.parseLong(parts[4]);
            int quantity = Integer.parseInt(parts[5]);

            // 1. Получаем сироп
            Product milk = productService.getProductById(milkId)
                    .orElseThrow(() -> new RuntimeException("Молоко не найден"));

            // 2. Получаем cartItem
            CartItem cartItem = cartItemService.getCartItemById(cartItemId);

            // 3. Добавляем сироп к cartItem
            // Сначала удаляем старый сироп, если есть
            Product oldMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);
            if (oldMilk != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldMilk.getId());
            }

            // Добавляем новый сироп
            cartItemAddonService.addAddonToCartItem(cartItem, milk, 1, milk.getAmount());

            // 4. Показываем результат
            String messageText = String.format("""
                ✅ *Молоко добавлен!*
                
                🍯 Вы выбрали: *%s*
                💰 Доплата: *+%d₽*
                
                Добавка успешно добавлена к вашему напитку.
                """,
                    milk.getName(),
                    milk.getAmount());

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("milk_addons_" + cartItemId + "_" + productId + "_" + quantity);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);
            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка выбора молока", e);
            sendMessage(chatId, "❌ Ошибка при добавлении молока");
        }
    }

    private void showMilksSelection(Long chatId, String data) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);

        Product currentMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);

        String currentSyrupText = currentMilk != null
                ? String.format("\n\nТекущий сироп: *%s* (+%d₽)",
                currentMilk.getName(), currentMilk.getAmount())
                : "";

        String messageText = String.format("""
            🍯 *Выбор сиропа*
            
            Выберите сироп для вашего напитка:%s
            """, currentSyrupText);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<Product> milks = productService.getAvailableMilks();

        if (milks.isEmpty()) {
            // Нет доступных сиропов
            InlineKeyboardButton unavailableButton = new InlineKeyboardButton("⚠️ Сиропы временно недоступны")
                    .callbackData("noop");
            keyboard.addRow(unavailableButton);
        } else {
            // Показываем сиропы с эмодзи
            for (Product milk : milks) {
                // Добавляем эмодзи к названию

                String buttonText = String.format("%s +%d₽",
                        milk.getName(), milk.getAmount());

                InlineKeyboardButton syrupButton = new InlineKeyboardButton(buttonText)
                        .callbackData("select_milk_" + cartItemId + "_" + milk.getId() + "_" + productId + "_" + quantity);
                keyboard.addRow(syrupButton);
            }
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                .callbackData("milk_addons_" + cartItemId + "_" + productId + "_" + quantity);
        keyboard.addRow(backButton);

        try {
            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);
            bot.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке выбора альтернативного молока", e);
            sendMessage(chatId, "❌ Ошибка при загрузке альтернативного молока");
        }
    }

    private void handleAddonsSelectionMilk(Long chatId, String data) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);

        Product syrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
        Product milk = cartItemAddonService.getMilkByCartItemId(cartItemId);

        String text = String.format("""
                       Добавки:
                       Сироп: %s
                       Альтернативное молоко: %s
                       Выберите действие:
                       """,
                (syrup == null) ? "Не добавлен": syrup.getName(),
                (milk == null) ? "Не добавлено": milk.getName()
        );
        // Создаем клавиатуру ТОЛЬКО с двумя кнопками для сиропа
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
        addSyrupButton.setText("➕ Добавить альт. молоко");
        addSyrupButton.setCallbackData("add_milk_" + cartItemId + "_" + productId + "_" + quantity);

        keyboardMarkup.addRow(addSyrupButton);


        if (milk != null) {
            InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
            removeSyrupButton.setText("➖ Убрать альт. молоко");
            // Активируем кнопку только если есть сироп
            removeSyrupButton.setCallbackData("remove_milk_" + cartItemId + "_" + productId + "_" + quantity);

            keyboardMarkup.addRow(removeSyrupButton);
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("addon_milk_" + productId + "_" + quantity);
        keyboardMarkup.addRow(backButton);


        try {
            SendMessage message = new SendMessage(chatId.toString(), text)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboardMarkup);
            bot.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения с добавками", e);
            sendMessage(chatId, "❌ Ошибка при загрузке добавок");
        }
    }

    private void removeSelectedSyrup(Long chatId, Integer messageId, String data) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.parseLong(parts[2]);

            Product oldSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
            if (oldSyrup != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldSyrup.getId());
            }

            // Обновляем сообщение с добавками вместо отправки нового
            updateAddonsMessage(chatId, messageId, data, "syrup");

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    private void updateAddonsMessage(Long chatId, Integer messageId, String data, String addon) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);

        Product syrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
        Product milk = cartItemAddonService.getMilkByCartItemId(cartItemId);

        String text = String.format("""
                       Добавки:
                       Сироп: %s
                       Альтернативное молоко: %s
                       Выберите действие:
                       """,
                (syrup == null) ? "Не добавлен": syrup.getName(),
                (milk == null) ? "Не добавлено": milk.getName()
        );
        InlineKeyboardMarkup keyboardMarkup;
        // Создаем клавиатуру ТОЛЬКО с двумя кнопками для сиропа
        if(addon.equals("syrup")) {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить сироп");
            addSyrupButton.setCallbackData("add_syrup_" + cartItemId + "_" + productId + "_" + quantity);

            keyboardMarkup.addRow(addSyrupButton);


            if (syrup != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать сироп");
                // Активируем кнопку только если есть сироп
                removeSyrupButton.setCallbackData("remove_syrup_" + cartItemId + "_" + productId + "_" + quantity);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("addon_syrup_" + productId + "_" + quantity);
            keyboardMarkup.addRow(backButton);
        }else {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить альт. молоко");
            addSyrupButton.setCallbackData("add_milk_" + cartItemId + "_" + productId + "_" + quantity);

            keyboardMarkup.addRow(addSyrupButton);


            if (milk != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать альт. молоко");
                // Активируем кнопку только если есть сироп
                removeSyrupButton.setCallbackData("remove_milk_" + cartItemId + "_" + productId + "_" + quantity);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("addon_milk_" + productId + "_" + quantity);
            keyboardMarkup.addRow(backButton);
        }
        try {

            EditMessageText editMessage = new EditMessageText(chatId, messageId, text)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboardMarkup);

            bot.execute(editMessage);
        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщения с добавками", e);
            sendMessage(chatId, "❌ Ошибка при обновлении добавок");
        }
    }

    private void handleSelectSyrup(Long chatId, String data) {
        try {
            // Пример данных: "select_syrup_22_45" (cartItemId=22, syrupId=45)
            String[] parts = data.split("_");
            Long cartItemId = Long.valueOf(parts[2]);
            Long syrupId = Long.valueOf(parts[3]);
            long productId = Long.parseLong(parts[4]);
            int quantity = Integer.parseInt(parts[5]);

            // 1. Получаем сироп
            Product syrup = productService.getProductById(syrupId)
                    .orElseThrow(() -> new RuntimeException("Сироп не найден"));

            // 2. Получаем cartItem
            CartItem cartItem = cartItemService.getCartItemById(cartItemId);

            // 3. Добавляем сироп к cartItem
            // Сначала удаляем старый сироп, если есть
            Product oldSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
            if (oldSyrup != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldSyrup.getId());
            }

            // Добавляем новый сироп
            cartItemAddonService.addAddonToCartItem(cartItem, syrup, 1, syrup.getAmount());

            // 4. Показываем результат
            String messageText = String.format("""
                ✅ *Сироп добавлен!*
                
                🍯 Вы выбрали: *%s*
                💰 Доплата: *+%d₽*
                
                Добавка успешно добавлена к вашему напитку.
                """,
                    syrup.getName(),
                    syrup.getAmount());

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("syrup_addons_" + cartItemId + "_" + productId + "_" + quantity);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);
            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    private void showSyrupsSelection(Long chatId, String data) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);

        Product currentSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);

        String currentSyrupText = currentSyrup != null
                ? String.format("\n\nТекущий сироп: *%s* (+%d₽)",
                currentSyrup.getName(), currentSyrup.getAmount())
                : "";

        String messageText = String.format("""
            🍯 *Выбор сиропа*
            
            Выберите сироп для вашего напитка:%s
            """, currentSyrupText);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<Product> syrups = productService.getAvailableSyrups();

        if (syrups.isEmpty()) {
            // Нет доступных сиропов
            InlineKeyboardButton unavailableButton = new InlineKeyboardButton("⚠️ Сиропы временно недоступны")
                    .callbackData("noop");
            keyboard.addRow(unavailableButton);
        } else {
            // Показываем сиропы с эмодзи
            for (Product syrup : syrups) {
                // Добавляем эмодзи к названию

                String buttonText = String.format("%s +%d₽",
                        syrup.getName(), syrup.getAmount());

                InlineKeyboardButton syrupButton = new InlineKeyboardButton(buttonText)
                        .callbackData("select_syrup_" + cartItemId + "_" + syrup.getId() + "_" + productId + "_" + quantity);
                keyboard.addRow(syrupButton);
            }
        }

        // Кнопка "Назад"
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                .callbackData("syrup_addons_" + cartItemId + "_" + productId + "_" + quantity);
        keyboard.addRow(backButton);

        try {
            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);
            bot.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке выбора сиропов", e);
            sendMessage(chatId, "❌ Ошибка при загрузке сиропов");
        }
    }

    private void handleAddonsSelectionSyrup(Long chatId, String data) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);

        Product syrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
        Product milk = cartItemAddonService.getMilkByCartItemId(cartItemId);

        String text = String.format("""
                       Добавки:
                       Сироп: %s
                       Альтернативное молоко: %s
                       Выберите действие:
                       """,
                (syrup == null) ? "Не добавлен": syrup.getName(),
                (milk == null) ? "Не добавлено": milk.getName()
                );
        // Создаем клавиатуру ТОЛЬКО с двумя кнопками для сиропа
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
        addSyrupButton.setText("➕ Добавить сироп");
        addSyrupButton.setCallbackData("add_syrup_" + cartItemId + "_" + productId + "_" + quantity);

        keyboardMarkup.addRow(addSyrupButton);


        if (syrup != null) {
            InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
            removeSyrupButton.setText("➖ Убрать сироп");
            // Активируем кнопку только если есть сироп
            removeSyrupButton.setCallbackData("remove_syrup_" + cartItemId + "_" + productId + "_" + quantity);

            keyboardMarkup.addRow(removeSyrupButton);
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("addon_syrup_" + productId + "_" + quantity);
        keyboardMarkup.addRow(backButton);


        try {
            SendMessage message = new SendMessage(chatId.toString(), text)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboardMarkup);
            bot.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения с добавками", e);
            sendMessage(chatId, "❌ Ошибка при загрузке добавок");
        }
    }

    /**
     * Обработка добавления добавок к товарам в корзине
     */
    private void handleCartAddSyrup(Long chatId, String data) {
        try {
            String[] parts = data.split("_");
            long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            // Получаем продукт по ID, чтобы узнать его название
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            // Получаем список товаров в корзине по имени продукта
            List<CartItem> cartItems = cartService.findCartItemByProduct(chatId, product.getName());

            if (cartItems == null || cartItems.isEmpty()) {
                sendMessage(chatId, "🛒 Товар '" + product.getName() + "' не найден в вашей корзине");
                return;
            }

            String messageText = """
                🍯 *Добавление сиропа к товарам в корзине*
                
                Выберите товар, к которому хотите добавить добавки:
                """;

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            // Создаем кнопки для каждого товара в корзине, который поддерживает добавки
            for (CartItem cartItem : cartItems) {
                product = cartItem.getProduct();

                if (keyboardService.needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("syrup_addons_" + cartItem.getId() + "_" + productId + "_" + quantity);  // ← ЗДЕСЬ ПЕРЕДАЕТСЯ cartItemId
                    keyboard.addRow(itemButton);
                }
            }

            // Кнопка "Назад в корзину"
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору добавок")
                    .callbackData("addons_" + productId + "_" + quantity);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при показе товаров для добавления добавок: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при загрузке товаров из корзины");
        }
    }

    /**
     * Обработка добавления добавок к товарам в корзине
     */
    private void handleCartAddMilk(Long chatId, String data) {
        try {
            String[] parts = data.split("_");
            long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            // Получаем продукт по ID, чтобы узнать его название
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            // Получаем список товаров в корзине по имени продукта
            List<CartItem> cartItems = cartService.findCartItemByProduct(chatId, product.getName());

            if (cartItems == null || cartItems.isEmpty()) {
                sendMessage(chatId, "🛒 Товар '" + product.getName() + "' не найден в вашей корзине");
                return;
            }

            String messageText = """
                🍯 *Добавление альтернативного молока к товарам в корзине*
                
                Выберите товар, к которому хотите добавить добавки:
                """;

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            // Создаем кнопки для каждого товара в корзине, который поддерживает добавки
            for (CartItem cartItem : cartItems) {
                product = cartItem.getProduct();

                if (keyboardService.needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("milk_addons_" + cartItem.getId() + "_" + productId + "_" + quantity);
                    keyboard.addRow(itemButton);
                }
            }

            // Кнопка "Назад в корзину"
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору добавок")
                    .callbackData("addons_" + productId + "_" + quantity);
            keyboard.addRow(backButton);

            SendMessage message = new SendMessage(chatId.toString(), messageText)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при показе товаров для добавления добавок: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при загрузке товаров из корзины");
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

            if(cartService.findProductInCart(chatId, productId)){
                sendMessage(chatId, "Сначала добавьте товар в корзину");
                return;
            }

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

            if (parts.length < 5) {
                log.error("Недостаточно параметров: {}", data);
                answerCallback(callbackId, "❌ Ошибка в данных");
                return;
            }

            Long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);

            log.info("Товар ID: {}, количество: {}", productId, quantity);

            // Получаем информацию о товаре
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

            // Добавляем товар в корзину
            List<CartItem> addedItems = cartService.addProductToCart(chatId, productId, quantity);

            if (!addedItems.isEmpty()) {
                // Берем первый добавленный элемент для отображения информации
                CartItem firstItem = addedItems.getFirst();

                String message = String.format(
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
            String cartDescription = cartService.getCartDescription(chatId);

            InlineKeyboardMarkup keyboard = keyboardService.createBasketKeyboard(chatId);

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

                    setLastMessageType(chatId);
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

            if (parts.length < 5) {
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
        String previousType = getAndUpdateLastMessageType(chatId);
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
                            DeleteMessage deleteMsg =
                                    new DeleteMessage(chatId, messageIdToDelete);
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
