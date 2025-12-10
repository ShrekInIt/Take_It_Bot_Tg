package com.example.bot.Telegram_bot_take_it.controller;

import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.UserService;
import com.example.bot.Telegram_bot_take_it.utils.KeyboardService;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class BotController {
    private static final Logger logger = LoggerFactory.getLogger(BotController.class);

    private final KeyboardService keyboardService;
    private final TelegramBot bot;
    private final CallbackHandlerController callbackHandler;
    private final UserService userService;
    private final CartService cartService;

    /**
     * Основной метод обработки входящих обновлений от Telegram
     * Распределяет обработку между сообщениями и callback-запросами
     */
    public void handleUpdate(Update update) {
        try {
            if (update.message() != null) {
                handleMessage(update.message());
            } else if (update.callbackQuery() != null) {
                handleCallbackQuery(update.callbackQuery());
            }
        } catch (Exception e) {
            logger.error("Error handling update: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка входящих текстовых сообщений
     */
    private void handleMessage(Message message) {
        Long chatId = message.chat().id();
        String text = message.text();

        if (text != null) {
            handleCommand(chatId, text, message.from());
        }
    }

    /**
     * Обработка callback-запросов от inline-кнопок
     */
    @SuppressWarnings("deprecation")
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        // Регистрируем/обновляем пользователя и для callback запросов
        registerOrUpdateUser(callbackQuery.from(), callbackQuery.message().chat().id());
        callbackHandler.handleCallbackQuery(callbackQuery);
    }

    /**
     * Обработка текстовых команд от пользователя
     */
    private void handleCommand(Long chatId, String text, com.pengrad.telegrambot.model.User telegramUser) {
        String command = text.trim().toLowerCase();

        logger.info("Command received: {} from chatId: {}", command, chatId);

        switch (command) {
            case "/start" -> handleStartCommand(chatId, telegramUser);
            case "/help" -> handleHelpCommand(chatId);
            case "/menu" -> handleMenuCommand(chatId);
            case "/photomenu" -> handlerPhotoMenu(chatId);
            case "/basket", "🛒 корзина", "корзина" -> handleBasketCommand(chatId, telegramUser); // Добавляем обработку корзины
            case Messages.MENU_LOWERCASE -> handleMenuCommandCategory(chatId);
            default -> handleUnknownCommand(chatId);
        }
    }

    /**
     * Обработка команды /basket - просмотр корзины
     */
    private void handleBasketCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        try {
            // Регистрируем/обновляем пользователя
            User user = registerOrUpdateUser(telegramUser, chatId);

            if (user == null) {
                sendMessage(chatId, "❌ Не удалось загрузить данные пользователя");
                return;
            }

            // Получаем содержимое корзины
            String cartDescription = cartService.getCartDescription(chatId);

            // Создаем клавиатуру для корзины
            InlineKeyboardMarkup keyboard = createBasketKeyboard(chatId);

            SendMessage message = new SendMessage(chatId.toString(), cartDescription)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            logger.error("Ошибка при просмотре корзины: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при загрузке корзины");
        }
    }

    /**
     * Создать клавиатуру для корзины
     */
    private InlineKeyboardMarkup createBasketKeyboard(Long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        // Проверяем, пуста ли корзина
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

        return keyboard;
    }

    /**
     * Регистрация или обновление пользователя
     */
    private User registerOrUpdateUser(com.pengrad.telegrambot.model.User telegramUser, Long chatId) {
        try {
            String telegramId = String.valueOf(telegramUser.id());
            String username = telegramUser.username();
            String firstName = telegramUser.firstName();


            // Создаем имя пользователя
            String displayName;
            if (username != null && !username.isEmpty()) {
                displayName = username;
            } else if (firstName != null && !firstName.isEmpty()) {
                displayName = firstName;
            } else {
                displayName = "User_" + telegramId;
            }

            // Проверяем, существует ли пользователь
            return userService.getUserByTelegramId(telegramId)
                    .map(existingUser -> {
                        // Обновляем существующего пользователя
                        boolean updated = false;

                        if (!displayName.equals(existingUser.getName())) {
                            existingUser.setName(displayName);
                            updated = true;
                        }

                        if (!chatId.equals(existingUser.getChatId())) {
                            existingUser.setChatId(chatId);
                            updated = true;
                        }

                        if (updated) {
                            existingUser = userService.updateUser(existingUser);
                            logger.info("Пользователь обновлен: {} (ID: {})", displayName, existingUser.getId());
                        }

                        return existingUser;
                    })
                    .orElseGet(() -> {
                        // Создаем нового пользователя
                        User newUser = userService.createUser(telegramId, displayName, chatId);
                        logger.info("Новый пользователь зарегистрирован: {} (ID: {})", displayName, newUser.getId());
                        return newUser;
                    });

        } catch (Exception e) {
            logger.error("Ошибка регистрации пользователя: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Обработка команды /start - приветствие и регистрация пользователя
     */
    private void handleStartCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        // Регистрируем/обновляем пользователя
        User user = registerOrUpdateUser(telegramUser, chatId);

        String welcomeText;

        if (user != null) {
            if (user.getCreatedAt() != null && user.getCreatedAt().isBefore(LocalDateTime.now().minusDays(1))) {
                welcomeText = String.format(
                        """
                        👋 *С возвращением, %s!*
                        
                        Рады видеть вас снова в нашем кофейном боте!
                        Используйте команду /menu для просмотра меню.
                        """,
                        telegramUser.firstName()
                );
            } else {
                welcomeText = String.format(
                        """
                        🎉 *Добро пожаловать, %s!*
                        
                        Рады приветствовать вас в нашем кофейном боте!
                        Используйте команду /menu для просмотра меню.
                        """,
                        telegramUser.firstName()

                );
            }
        } else {
            welcomeText = Messages.HELLO_TEXT;
        }

        sendMessage(chatId, welcomeText);

        // Отправляем основное меню после приветствия
        ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();
        SendMessage menuMessage = new SendMessage(chatId.toString(),
                "👇 *Используйте кнопки ниже для навигации:*")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeRequest(menuMessage, chatId);
    }

    /**
     * Обработка команды /help - вывод справки
     */
    private void handleHelpCommand(Long chatId) {
        String helpText = Messages.HELP_TEXT;
        sendMessage(chatId, helpText);
    }

    /**
     * Обработка неизвестной команды
     */
    private void handleUnknownCommand(Long chatId) {
        String unknownText = """
            🤔 *Неизвестная команда*
            
            Доступные команды:
            /start - Начать работу с ботом
            /help - Помощь по командам
            /menu - Главное меню
            
            Используйте кнопку "Меню" для навигации.
            """;

        sendMessage(chatId, unknownText);
    }

    /**
     * Обработка команды /menu - вывод справки
     */
    private void handleMenuCommand(Long chatId) {
        ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();

        // Отправляем сообщение с клавиатурой
        SendMessage request = new SendMessage(chatId.toString(), "🍽️ *Главное меню*\n\nВыберите действие:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeRequest(request, chatId);
    }

    /**
     * Обработка команды /photomenu - отправка фото меню
     */
    private void handlerPhotoMenu(Long chatId) {
        try {
            InputMediaPhoto[] mediaGroup = new InputMediaPhoto[2];

            java.io.File file1 = new java.io.File("src/main/resources/static/images/menu/photoCoffee.jpg");
            mediaGroup[0] = new InputMediaPhoto(file1)
                    .caption("☕ Кофейное меню");

            java.io.File file2 = new java.io.File("src/main/resources/static/images/menu/photoNotCoffee.jpg");
            mediaGroup[1] = new InputMediaPhoto(file2)
                    .caption("Не кофейное меню");

            SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId.toString(), mediaGroup);
            bot.execute(sendMediaGroup);

        } catch (Exception e) {
            logger.error("Ошибка отправки медиагруппы: {}", e.getMessage());
        }
    }

    /**
     * Обработка команды /menu - вывод главного меню с Reply-клавиатурой
     */
    private void handleMenuCommandCategory(Long chatId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            sendMessage(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:\n\n📭 В настоящее время нет доступных категорий.");
            return;
        }

        SendMessage request = new SendMessage(chatId.toString(), "🍽️ *Главное меню*\n\nВыберите категорию:")
                .parseMode(ParseMode.Markdown)
                .replyMarkup(keyboard);

        executeRequest(request, chatId);
    }

    /**
     * Отправка текстового сообщения пользователю
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage request = new SendMessage(chatId.toString(), text)
                .parseMode(ParseMode.Markdown);

        responser(chatId, request);
    }

    /**
     * Выполнение запроса на отправку сообщения с обработкой ошибок
     */
    private void responser(Long chatId, SendMessage request) {
        try {
            SendResponse response = bot.execute(request);

            if (response.isOk()) {
                logger.info("✅ Message sent to chatId: {}", chatId);
            } else {
                logger.error("❌ Send failed for chatId {}: {} - {}",
                        chatId, response.errorCode(), response.description());
            }
        } catch (Exception e) {
            logger.error("⚠️ Network error for chatId {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Обертка для выполнения запроса отправки сообщения
     */
    private void executeRequest(SendMessage request, Long chatId) {
        responser(chatId, request);
    }
}