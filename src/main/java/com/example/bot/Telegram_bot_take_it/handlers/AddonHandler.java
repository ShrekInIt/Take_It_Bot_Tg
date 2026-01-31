package com.example.bot.Telegram_bot_take_it.handlers;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.*;
import com.example.bot.Telegram_bot_take_it.utils.MessageSender;
import com.example.bot.Telegram_bot_take_it.utils.TelegramMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddonHandler {

    private final KeyboardService keyboardService;
    private final ProductService productService;
    private final CartService cartService;
    private final CartItemService cartItemService;
    private final CartItemAddonService cartItemAddonService;
    private final MessageSender messageSender;
    private final SyrupPriceService syrupPriceService;
    private final TelegramMessageSender telegramMessageSender;

    /**
     * Обработка callback запросов для добавок
     */
    public void handlerAddonCallback(Long chatId, Integer messageId, String data){
        if (data.startsWith("addons_show_edit_")) {
            System.out.println("We here");
            handleAddonsSelection(chatId, data, messageId);
        }
        else if (data.startsWith("addons_show_")) {
            log.info("Обработка выбора добавок...");
            handleAddonsSelection(chatId, data);
        }
        else if (data.startsWith("addons_milk_")) {
            handleAddonsSelectionMilk(chatId, data, messageId);
        }
        else if (data.startsWith("addons_syrup_")) {
            handleAddonsSelectionSyrup(chatId, data, messageId);
        }
        else if (data.startsWith("addons_add_syrup_")) {
            showSyrupsSelection(chatId, data, messageId);
        }
        else if (data.startsWith("addons_add_milk_")) {
            showMilksSelection(chatId, data, messageId);
        }
        else if (data.startsWith("addons_remove_syrup_")) {
            removeSelectedSyrup(chatId, messageId, data);
        }
        else if (data.startsWith("addons_remove_milk_")) {
            removeSelectedMilk(chatId, messageId, data);
        }
        else if (data.startsWith("addons_select_milk_")) {
            handleSelectMilk(chatId, data, messageId);
        }
        else if (data.startsWith("addons_select_syrup_")) {
            handleSelectSyrup(chatId, data, messageId);
        }
    }

    /**
     * Обработка выбора добавок (только для кофе)
     */
    private void handleAddonsSelection(Long chatId, String data) {
        try {
            log.info("Обработка выбора добавок, данные в handleAddonsSelection: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 3) {
                log.error("Недостаточно параметров в handleAddonsSelection: {}", data);
                messageSender.sendMessage(chatId, "❌ Ошибка в данных добавок");
                return;
            }

            Long productId = Long.parseLong(parts[2]);
            int quantity = Integer.parseInt(parts[3]);
            Long categoryId = Long.parseLong(parts[4]);

            if(cartService.findProductInCart(chatId, productId)){
                messageSender.sendMessage(chatId, "Сначала добавьте товар в корзину");
                return;
            }

            log.info("Товар ID в handleAddonsSelection: {}, количество: {}", productId, quantity);

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

                        telegramMessageSender.sendMessageWithInlineKeyboard(chatId, messageText, keyboardService.createAddonsKeyboard(productId, quantity, categoryId), true);
                    }
            );

        } catch (Exception e) {
            log.error("Ошибка выбора добавок: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при выборе добавок");
        }
    }

    /**
     * Обработка выбора добавок (только для кофе)
     */
    private void handleAddonsSelection(Long chatId, String data, Integer messageId) {
        try {
            log.info("Обработка выбора добавок, данные: {}", data);
            String[] parts = data.split("_");

            if (parts.length < 3) {
                log.error("Недостаточно параметров: {}", data);
                messageSender.sendMessage(chatId, "❌ Ошибка в данных добавок");
                return;
            }

            Long productId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);
            Long categoryId = Long.parseLong(parts[5]);

            if(cartService.findProductInCart(chatId, productId)){
                messageSender.sendMessage(chatId, "Сначала добавьте товар в корзину");
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

                        telegramMessageSender.sendEditMessage(chatId, messageId, messageText, keyboardService.createAddonsKeyboard(productId, quantity, categoryId), true);
                    }
            );

        } catch (Exception e) {
            log.error("Ошибка выбора добавок: {}", e.getMessage(), e);
            messageSender.sendMessage(chatId, "❌ Ошибка при выборе добавок");
        }
    }

    /**
     * Обновление сообщения с добавками
     */
    private void updateAddonsMessage(Long chatId, Integer messageId, String data, String addon) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[3]);
        long productId = Long.parseLong(parts[4]);
        int quantity = Integer.parseInt(parts[5]);
        long categoryId = Long.parseLong(parts[6]);

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
        try {
            telegramMessageSender.sendEditMessage(chatId, messageId, text, keyboardService.createKeyboardSyrupAndMilk(cartItemId, productId, quantity,categoryId, addon, syrup, milk), true);
        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщения с добавками", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при обновлении добавок");
        }
    }

    /**
     * Удаление выбранного молока
     */
    private void removeSelectedMilk(Long chatId, Integer messageId, String data) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.parseLong(parts[3]);

            Product oldMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);
            if (oldMilk != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldMilk.getId());
            }

            updateAddonsMessage(chatId, messageId, data, "milk");

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    /**
     * Выбор альтернативного молока
     */
    private void handleSelectMilk(Long chatId, String data, Integer messageId) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.valueOf(parts[3]);
            Long milkId = Long.valueOf(parts[4]);
            long productId = Long.parseLong(parts[5]);
            int quantity = Integer.parseInt(parts[6]);
            long categoryId = Long.parseLong(parts[7]);

            Product milk = productService.getProductById(milkId)
                    .orElseThrow(() -> new RuntimeException("Молоко не найден"));

            CartItem cartItem = cartItemService.getCartItemById(cartItemId);

            Product oldMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);
            if (oldMilk != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldMilk.getId());
            }

            cartItemAddonService.addAddonToCartItem(cartItem, milk, 1, milk.getAmount());

            String messageText = String.format("""
                ✅ *Молоко добавлен!*
                
                🍯 Вы выбрали: *%s*
                💰 Доплата: *+%d₽*
                
                Добавка успешно добавлена к вашему напитку.
                """,
                    milk.getName(),
                    milk.getAmount());

            telegramMessageSender.sendEditMessage(chatId, messageId,messageText,
                    keyboardService.createButtonBackForAddonsMilkOrSyrup(cartItemId, productId, quantity, categoryId, "milk"), true);

        } catch (Exception e) {
            log.error("Ошибка выбора молока", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении молока");
        }
    }

    /**
     * Показ выбранного молока
     */
    private void showMilksSelection(Long chatId, String data, Integer messageId) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[3]);
        long productId = Long.parseLong(parts[4]);
        int quantity = Integer.parseInt(parts[5]);
        long categoryId = Long.parseLong(parts[6]);

        Product currentMilk = cartItemAddonService.getMilkByCartItemId(cartItemId);

        String currentSyrupText = currentMilk != null
                ? String.format("\n\nТекущий сироп: *%s* (+%d₽)",
                currentMilk.getName(), currentMilk.getAmount())
                : "";

        String messageText = String.format("""
            🍯 *Выбор  альт.молока*
            
            Выберите альт.молоко для вашего напитка:%s
            """, currentSyrupText);

        try {
            telegramMessageSender.sendEditMessage(chatId, messageId, messageText,
                    keyboardService.createKeyboardForSelectedMilkOrSyrup(cartItemId, productId, quantity, categoryId, "milk", null), true);
        } catch (Exception e) {
            log.error("Ошибка при отправке выбора альтернативного молока", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке альтернативного молока");
        }
    }

    /**
     * Показ альтернативного молока
     */
    private void handleAddonsSelectionMilk(Long chatId, String data, Integer messageId) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);
        long categoryId = Long.parseLong(parts[5]);

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

        try {
            telegramMessageSender.sendEditMessage(chatId, messageId, text,
                    keyboardService.createKeyboardForMilkActionOrSyrupAction(cartItemId, productId, quantity, categoryId, milk, "milk"), true);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения с добавками", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке добавок");
        }
    }

    /**
     * Удаление выбранного сиропа
     */
    private void removeSelectedSyrup(Long chatId, Integer messageId, String data) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.parseLong(parts[3]);

            Product oldSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
            if (oldSyrup != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldSyrup.getId());
            }

            updateAddonsMessage(chatId, messageId, data, "syrup");

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    /**
     * Выбор альтернативного сиропа
     */
    private void handleSelectSyrup(Long chatId, String data, Integer messageId) {
        try {
            String[] parts = data.split("_");
            Long cartItemId = Long.valueOf(parts[3]);
            Long syrupId = Long.valueOf(parts[4]);
            long productId = Long.parseLong(parts[5]);
            int quantity = Integer.parseInt(parts[6]);
            long categoryId = Long.parseLong(parts[7]);

            Product syrup = productService.getProductById(syrupId)
                    .orElseThrow(() -> new RuntimeException("Сироп не найден"));

            CartItem cartItem = cartItemService.getCartItemById(cartItemId);

            Product oldSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);
            if (oldSyrup != null) {
                cartItemAddonService.deleteAddonByCartItemIdAndProductId(cartItemId, oldSyrup.getId());
            }

            cartItemAddonService.addAddonToCartItem(cartItem, syrup, 1, syrup.getAmount());

            String messageText = String.format("""
                ✅ *Сироп добавлен!*
                
                🍯 Вы выбрали: *%s*
                💰 Доплата: *+%d₽*
                
                Добавка успешно добавлена к вашему напитку.
                """,
                    syrup.getName(),
                    syrup.getAmount());

            telegramMessageSender.sendEditMessage(chatId, messageId, messageText,
                    keyboardService.createButtonBackForAddonsMilkOrSyrup(cartItemId, productId, quantity, categoryId, "syrup"), true);

        } catch (Exception e) {
            log.error("Ошибка выбора сиропа", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при добавлении сиропа");
        }
    }

    /**
     * Показ выбранного сиропа
     */
    private void showSyrupsSelection(Long chatId, String data, Integer messageId) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[3]);
        long productId = Long.parseLong(parts[4]);
        int quantity = Integer.parseInt(parts[5]);
        long categoryId = Long.parseLong(parts[6]);

        Product mainProduct = productService.getProductById(productId).orElseThrow(() -> new RuntimeException("Напиток не найден"));

        Product currentSyrup = cartItemAddonService.getSyrupByCartItemId(cartItemId);

        String currentSyrupText = "";
        if (currentSyrup != null) {
            int currentSyrupPrice = syrupPriceService.calculateSyrupPriceForSize(currentSyrup, mainProduct);
            currentSyrupText = String.format("\n\nТекущий сироп: *%s* (+%d₽)",
                    currentSyrup.getName(), currentSyrupPrice);
        }

        String messageText = String.format("""
            🍯 *Выбор сиропа*
            
            Выберите сироп для вашего напитка:%s
            """, currentSyrupText);

        try {
            telegramMessageSender.sendEditMessage(chatId, messageId,messageText,
                    keyboardService.createKeyboardForSelectedMilkOrSyrup(cartItemId, productId, quantity, categoryId, "syrup", mainProduct), true);
        } catch (Exception e) {
            log.error("Ошибка при отправке выбора сиропов", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке сиропов");
        }
    }

    /**
     * Показ альтернативного сиропа
     */
    private void handleAddonsSelectionSyrup(Long chatId, String data, Integer messageId) {
        String[] parts = data.split("_");
        Long cartItemId = Long.valueOf(parts[2]);
        long productId = Long.parseLong(parts[3]);
        int quantity = Integer.parseInt(parts[4]);
        long categoryId = Long.parseLong(parts[5]);

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

        try {
            telegramMessageSender.sendEditMessage(chatId, messageId, text,
                    keyboardService.createKeyboardForMilkActionOrSyrupAction(cartItemId, productId, quantity, categoryId,syrup, "syrup"), true);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения с добавками", e);
            messageSender.sendMessage(chatId, "❌ Ошибка при загрузке добавок");
        }
    }
}
