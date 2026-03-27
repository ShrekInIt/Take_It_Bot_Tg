package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.TelegramUserDto;
import com.example.bot.Telegram_bot_take_it.dto.response.UserResponseDto;
import com.example.bot.Telegram_bot_take_it.mapper.UserMapper;
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

    private final KeyboardService keyboardService;
    private final TelegramMessageSender messageSender;
    private final CartService cartService;
    private final OrderHistoryHandler orderHistoryHandler;
    private final TelegramUserRegistrar telegramUserRegistrar;
    private final UserMapper userMapper;

    /**
     * Обработка команды /start
     */
    public void handleStartCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        log.info("Обработка /start для chatId: {}", chatId);
        try {
            TelegramUserDto telegramUserDto = convertToTelegramUserDto(telegramUser);
            log.info("DTO создан: id={}, username={}", telegramUserDto.getId(), telegramUserDto.getUsername());

            User user = telegramUserRegistrar.registerOrUpdate(telegramUser, chatId);

            if (user != null) {
                UserResponseDto userDto = userMapper.toResponseDto(user);
                log.info("Пользователь создан/обновлен: id={}, name={}", userDto.getId(), userDto.getName());
                sendWelcomeMessage(chatId, userDto);
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
            log.error("Ошибка отправки медиа группы: {}", e.getMessage());
        }
    }

    /**
     * Обработка команды /basket - просмотр корзины
     */
    public void handleBasketCommand(Long chatId, com.pengrad.telegrambot.model.User telegramUser) {
        try {
            TelegramUserDto telegramUserDto = convertToTelegramUserDto(telegramUser);

            User user = telegramUserRegistrar.registerOrUpdate(telegramUserDto, chatId);

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
     * Формирует DTO пользователя Telegram для дальнейшей обработки в системе
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
     * Отправляет приветственное сообщение пользователю с клавиатурой главного меню
     */
    private void sendWelcomeMessage(Long chatId, UserResponseDto user) {
        if (user == null) {
            messageSender.sendMessage(chatId, "❌ Ошибка регистрации. Попробуйте еще раз.");
            return;
        }

        String welcomeText =
                "👋 Добро пожаловать, " + user.getName() + "!\n\n" +

                        "При использовании бота вы соглашаетесь с политикой конфиденциальности " +
                        "и добровольно даёте согласие на  обработку персональных данных. \n" +
                        "\n" +
                        "По вопросам конфиденциальности и отзыва согласия обращаться";

        messageSender.sendMessageWithReplyKeyboardHtml(chatId, welcomeText, keyboardService.getMainMenuKeyboard(), true);
    }

    /**
     * Получение всех заказов пользователя по chatId
     */
    public void getAllOrdersUser(Long chatId) {
        orderHistoryHandler.handleOrderHistory(chatId);
    }

    /**
     * Отправляет сообщение о нас
     */
    public void handleInfoAboutUs(Long chatId) {
        messageSender.sendMessage(chatId, Messages.ABOUT_US_TEXT);
    }
}
