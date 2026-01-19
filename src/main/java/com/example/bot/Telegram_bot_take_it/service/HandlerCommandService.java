package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.entity.User;
import com.example.bot.Telegram_bot_take_it.handlers.OrderHistoryHandler;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HandlerCommandService {

    private final UserTransactionService userTransactionService;
    private final KeyboardService keyboardService;
    private final TelegramMessageSender messageSender;
    private final CartService cartService;
    private final OrderHistoryHandler orderHistoryHandler;

    /**
     * Обработка команды /start
     */
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

        messageSender.sendMessageWithInlineKeyboard(chatId, "🍽️ *Главное меню*\n\nВыберите категорию:", keyboard, true);
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

           messageSender.sendMediaGroup(chatId, mediaGroup);

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

            messageSender.sendMessageWithInlineKeyboard(chatId, cartService.getCartDescription(chatId),
                    keyboardService.createBasketKeyboard(chatId), true);

        } catch (Exception e) {
            log.error("Ошибка при просмотре корзины: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке корзины");
        }
    }

    /**
     * Конвертация в DTO
     */
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

    /**
     * Отправление начального сообщения
     */
    private void sendWelcomeMessage(Long chatId, User user) {
        if (user == null) {
            messageSender.sendMessage(chatId, "❌ Ошибка регистрации. Попробуйте еще раз.");
            return;
        }

        String welcomeText = "👋 Добро пожаловать, " + user.getName() + "!\n\n" +
                "При использовании бота вы соглашаетесь с политикой конфиденциальности! \n"+
                " *\uD83D\uDD10 Политика конфиденциальности*\n" +
                "\n" +
                "*1. Какие данные мы собираем:*\n" +
                "• Номер телефона (только при оформлении заказа)\n" +
                "• История заказов\n" +
                "\n" +
                "*2. Как мы используем данные:*\n" +
                "• Для связи по вашему заказу\n" +
                "• Для информирования о статусе заказа\n" +
                "\n" +
                "*3. Как мы защищаем данные:*\n" +
                "• Все данные хранятся на защищенных серверах\n" +
                "• Доступ имеют только уполномоченные сотрудники\n" +
                "\n" +
                "*4. Ваши права:*\n" +
                "• Запросить удаление данных\n" +
                "• Получить информацию о хранимых данных\n" +
                "• Отказаться от обработки\n" +
                "\n" +
                "*5. Контакты:*\n" +
                "По вопросам конфиденциальности обращайтесь:\n" +
                "\uD83D\uDCE7 @.....\n" +
                "\uD83D\uDCDE +7 (999) 123-45-67\n" +
                "\n" +
                "*Действующая версия от 14.01.2026*\n" +
                "Вы успешно зарегистрированы в боте.\n" +
                "Используйте меню для выбора товаров.";

        messageSender.sendMessageWithReplyKeyboardHtml(chatId, welcomeText, keyboardService.getMainMenuKeyboard(), true);
    }

    /**
     * Получение всех заказов пользователя по chatId
     */
    public void getAllOrdersUser(Long chatId) {
        orderHistoryHandler.handleOrderHistory(chatId);
    }

    public void handleInfoAboutUs(Long chatId) {
        messageSender.sendMessage(chatId, Messages.ABOUT_US_TEXT);
    }
}
