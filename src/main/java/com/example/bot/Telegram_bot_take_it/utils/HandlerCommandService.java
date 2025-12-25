package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.UserTransactionService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HandlerCommandService {

    private final TelegramBot bot;
    private final UserTransactionService userTransactionService;
    private final KeyboardService keyboardService;
    private final TelegramMessageSender messageSender;
    private final CartService cartService;

    public void handleStartCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        log.info("Обработка /start для chatId: {}", chatId);

        try {
            TelegramUserDto telegramUserDto = convertToTelegramUserDto(telegramUser);
            log.info("DTO создан: id={}, username={}", telegramUserDto.getId(), telegramUserDto.getUsername());

            User user = userTransactionService.registerOrUpdateUser(telegramUserDto, chatId);

            if (user != null) {
                log.info("Пользователь создан/обновлен: id={}, name={}", user.getId(), user.getName());
                sendWelcomeMessage(chatId, user);
            } else {
                log.error("Не удалось создать/обновить пользователя");
                messageSender.sendMessage(chatId, "❌ Ошибка регистрации. Попробуйте еще раз.");
            }
        } catch (Exception e) {
            log.error("Ошибка в handleStartCommand: ", e);
            messageSender.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
        }
    }

    /**
     * Обработка команды /menu - вывод главного меню с Reply-клавиатурой
     */
    public void handleMenuCommandCategory(Long chatId) {
        InlineKeyboardMarkup keyboard = keyboardService.getCategoryKeyboard(null);

        if (keyboard == null) {
            messageSender.sendMessage(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:\n\n📭 В настоящее время нет доступных категорий.");
            return;
        }

        messageSender.sendMessageWithInlineKeyboard(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:", keyboard);
    }

    /**
     * Обработка команды /photomenu - отправка фото меню
     */
    public void handlerPhotoMenu(Long chatId) {
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
            log.error("Ошибка отправки медиагруппы: {}", e.getMessage());
        }
    }

    /**
     * Обработка команды /basket - просмотр корзины
     */
    public void handleBasketCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        try {
            TelegramUserDto telegramUserDto = convertToTelegramUserDto(telegramUser);

            User user = userTransactionService.registerOrUpdateUser(telegramUserDto, chatId);

            if (user == null) {
                messageSender.sendMessage(chatId, "❌ Не удалось загрузить данные пользователя");
                return;
            }

            String cartDescription = cartService.getCartDescription(chatId);

            InlineKeyboardMarkup keyboard = keyboardService.createBasketKeyboard(chatId);

            SendMessage message = new SendMessage(chatId.toString(), cartDescription)
                    .parseMode(ParseMode.Markdown)
                    .replyMarkup(keyboard);

            bot.execute(message);

        } catch (Exception e) {
            log.error("Ошибка при просмотре корзины: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке корзины");
        }
    }

    /**
     * Обработка неизвестной команды
     */
    public void handleUnknownCommand(Long chatId) {
        String unknownText = """
            🤔 *Неизвестная команда*
            
            Доступные команды:
            /start - Начать работу с ботом
            /help - Помощь по командам
            /menu - Главное меню
            
            Используйте кнопку "Меню" для навигации.
            """;

        messageSender.sendMessage(chatId, unknownText);
    }

    private TelegramUserDto convertToTelegramUserDto(com.pengrad.telegrambot.model.User telegramUser) {
        return TelegramUserDto.builder()
                .id(telegramUser.id())
                .username(telegramUser.username())
                .firstName(telegramUser.firstName())
                .lastName(telegramUser.lastName())
                .isBot(telegramUser.isBot())
                .languageCode(telegramUser.languageCode())
                .build();
    }

    private void sendWelcomeMessage(Long chatId, User user) {
        if (user == null) {
            messageSender.sendMessage(chatId, "❌ Ошибка регистрации. Попробуйте еще раз.");
            return;
        }

        String welcomeText = "👋 Добро пожаловать, " + user.getName() + "!\n\n" +
                "Вы успешно зарегистрированы в боте.\n" +
                "Используйте меню для выбора товаров.";

        ReplyKeyboardMarkup keyboard = keyboardService.getMainMenuKeyboard();

        messageSender.sendMessageWithReplyKeyboard(chatId, welcomeText, keyboard);
    }
}
