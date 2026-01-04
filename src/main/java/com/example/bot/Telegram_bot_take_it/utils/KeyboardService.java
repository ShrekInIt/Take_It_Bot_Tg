package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.dto.CartItemGroupDTO;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.CartService;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyboardService {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CartService cartService;

    // Кэш для фото: путь → байты
    private final Map<String, byte[]> photoCache = new ConcurrentHashMap<>();

    /**
     * Список названий кофе, для которых НЕ нужны добавки
     */
    private static final Set<String> COFFEE_WITHOUT_ADDONS = Set.of(
            "Эспрессо", "Американо"
    );

    /**
     * Проверить, нужны ли добавки для продукта
     */
    public boolean needsAddons(Product product) {
        if (product == null) {
            return false;
        }

        boolean isCoffee = categoryService.isCoffeeCategoryById(product.getCategoryId());
        if (!isCoffee) {
            return false;
        }

        String productName = product.getName().toLowerCase();

        if (COFFEE_WITHOUT_ADDONS.stream()
                .anyMatch(ex -> productName.contains(ex.toLowerCase()))) {
            return false;
        }

        Set<Long> excludedIds = Set.of(4L, 9L);
        return !excludedIds.contains(product.getId());
    }

    /**
     * Создать клавиатуру для выбора добавок
     */
    public InlineKeyboardMarkup createAddonsKeyboard(Long productId, int quantity, Long categoryId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        Long syrupCategoryId = 20L; // Сиропы
        Long milkCategoryId = 21L;  // Альтернативное молоко

        List<Product> syrups = productService.getAvailableProductsWithStock(syrupCategoryId);
        log.info("Найдено сиропов: {}", syrups.size());

        if (!syrups.isEmpty()) {
            InlineKeyboardButton syrupButton = new InlineKeyboardButton("🍯 Сиропы")
                    .callbackData("cart_addon_syrup_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(syrupButton);
        }

        List<Product> milks = productService.getAvailableProductsWithStock(milkCategoryId);
        log.info("Найдено видов молока: {}", milks.size());

        if (!milks.isEmpty()) {
            InlineKeyboardButton milkButton = new InlineKeyboardButton("🥛 Альтернативное молоко")
                    .callbackData("cart_addon_milk_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(milkButton);
        }

        if (keyboard.inlineKeyboard() == null || keyboard.inlineKeyboard().length == 0) {
            InlineKeyboardButton noAddonsButton = new InlineKeyboardButton("⚠️ Добавки временно недоступны")
                    .callbackData("no_action");
            keyboard.addRow(noAddonsButton);
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к товару")
                .callbackData("product_" + productId + "_" + categoryId);
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создать клавиатуру для товара (только кнопки!)
     */
    public InlineKeyboardMarkup createProductKeyboard(Product product, int quantity, Long sourceCategoryId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        Long productId = product.getId();
        String callbackPrefix = (sourceCategoryId != null) ?
                productId + "_" + quantity + "_" + sourceCategoryId :
                productId + "_" + quantity + "_null";


        InlineKeyboardButton minusButton = new InlineKeyboardButton("➖")
                .callbackData("quantity_minus_" + callbackPrefix);

        InlineKeyboardButton quantityButton = new InlineKeyboardButton(quantity + " шт")
                .callbackData("quantity_display_" + productId + "_" + quantity);

        InlineKeyboardButton plusButton = new InlineKeyboardButton("➕")
                .callbackData("quantity_plus_" + callbackPrefix);

        keyboard.addRow(minusButton, quantityButton, plusButton);

        if (needsAddons(product)) {
            InlineKeyboardButton addonsButton = new InlineKeyboardButton("🍯 Добавки")
                    .callbackData("addons_show_" + productId + "_" + quantity + "_" + sourceCategoryId);
            keyboard.addRow(addonsButton);
        }

        InlineKeyboardButton addToCartButton = new InlineKeyboardButton("🛒 Добавить в корзину")
                .callbackData("cart_add_" + product.getId() + "_" + quantity);
        keyboard.addRow(addToCartButton);

        InlineKeyboardButton basketButton = new InlineKeyboardButton("🛒 В корзину")
                .callbackData("cart_back");
        keyboard.addRow(basketButton);

        String backCallback;
        if (sourceCategoryId != null) {
            backCallback = "category_" + sourceCategoryId;
        } else {
            backCallback = "category_null";
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData(backCallback);
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создание клавиатуры для пустой корзины
     */
    private InlineKeyboardMarkup createEmptyCartKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton menuButton = new InlineKeyboardButton("📋 Перейти в меню")
                .callbackData("cart_back_to_menu");

        InlineKeyboardButton mainButton = new InlineKeyboardButton("🏠 На главную")
                .callbackData("main_menu");

        keyboard.addRow(menuButton);
        keyboard.addRow(mainButton);

        return keyboard;
    }

    /**
     * Создание клавиатуры с товарами из корзины
     * @param chatId ID чата пользователя
     * @return InlineKeyboardMarkup с товарами из корзины
     */
    public InlineKeyboardMarkup createCartProductsKeyboard(Long chatId) {
        try {
            List<CartItem> cartItems = cartService.getCartItems(chatId);

            if (cartItems.isEmpty()) {
                log.info("Корзина пуста для пользователя с chatId {}", chatId);
                return createEmptyCartKeyboard();
            }

            Map<String, CartItemGroupDTO> groupedItems = new LinkedHashMap<>();

            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                if (product == null) {
                    log.error("Продукт не найден для CartItem ID: {}", cartItem.getId());
                    continue;
                }

                String groupKey = createGroupKey(cartItem);

                groupedItems.computeIfAbsent(groupKey, k ->
                                new CartItemGroupDTO(product, new ArrayList<>(), cartItem.getAddons()))
                        .getItems().add(cartItem);
            }

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

            for (Map.Entry<String, CartItemGroupDTO> entry : groupedItems.entrySet()) {
                CartItemGroupDTO group = entry.getValue();
                Product product = group.getProduct();

                int totalQuantity = 0;
                int totalPrice = 0;

                for (CartItem cartItem : group.getItems()) {
                    totalQuantity += cartItem.getCountProduct();
                    totalPrice += cartItem.calculateItemTotal();
                }

                String buttonText = formatGroupedItemText(product, group, totalQuantity, totalPrice);

                CartItem firstItem = group.getItems().getFirst();

                InlineKeyboardButton productButton = new InlineKeyboardButton(buttonText)
                        .callbackData("cart_product_" + firstItem.getId());

                keyboard.addRow(productButton);
            }

            InlineKeyboardButton clearCartButton = new InlineKeyboardButton("🗑️ Очистить корзину")
                    .callbackData("cart_clear");

            InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Назад в корзину")
                    .callbackData("cart_back");

            keyboard.addRow(clearCartButton);
            keyboard.addRow(backButton);

            return keyboard;

        } catch (Exception e) {
            log.error("Ошибка при создании клавиатуры корзины для chatId {}: {}", chatId, e.getMessage(), e);
            return createEmptyCartKeyboard();
        }
    }

    /**
     * Создает ключ для группировки товаров
     */
    private String createGroupKey(CartItem cartItem) {
        StringBuilder key = new StringBuilder(cartItem.getProduct().getName());

        if (!cartItem.getAddons().isEmpty()) {
            key.append("_addons:");
            cartItem.getAddons().stream()
                    .sorted(Comparator.comparing(a -> a.getAddonProduct().getName()))
                    .forEach(addon -> key.append(addon.getAddonProduct().getId()).append("_"));
        }

        return key.toString();
    }

    /**
     * Форматирует текст для сгруппированного товара как в примере
     */
    private String formatGroupedItemText(Product product, CartItemGroupDTO group, int totalQuantity, int totalPrice) {
        StringBuilder text = new StringBuilder();

        text.append(product.getName());

        if (!group.getAddons().isEmpty()) {
            List<String> addonNames = group.getAddons().stream()
                    .map(addon -> addon.getAddonProduct().getName())
                    .toList();

            text.append(" (").append(String.join(", ", addonNames)).append(")");
        }

        text.append(" (").append(totalQuantity).append(" шт.) - ").append(totalPrice).append("₽");

        return text.toString();
    }

    /**
     * Прочитать файл из пути в поле photo
     */
    public byte[] readPhotoFile(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return null;
        }

        if (photoCache.containsKey(photoPath)) {
            log.debug("Фото из кэша: {}", photoPath);
            return photoCache.get(photoPath);
        }

        try {
            byte[] photoBytes = null;

            java.io.File file = new java.io.File(photoPath);
            if (file.exists() && file.isFile()) {
                photoBytes = Files.readAllBytes(file.toPath());
                log.debug("Фото загружено с диска: {} ({} байт)", photoPath, photoBytes.length);
            }
            else {
                String resourcePath = photoPath.replace("src/main/resources/", "");
                try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    if (stream != null) {
                        photoBytes = stream.readAllBytes();
                        log.debug("Фото загружено из ресурсов: {} ({} байт)", resourcePath, photoBytes.length);
                    }
                }
            }

            photoCache.put(photoPath, photoBytes);
            return photoBytes;

        } catch (Exception e) {
            log.error("Ошибка чтения фото: {}", e.getMessage());
            photoCache.put(photoPath, null);
            return null;
        }
    }

    /**
     * Создать клавиатуру с категориями для указанного родителя
     * Кэширует результат для оптимизации
     */
    @Cacheable(value = "categoryKeyboards", key = "#parentId")
    @Transactional(readOnly = true)
    public InlineKeyboardMarkup getCategoryKeyboard(Long parentId) {
        var categories = (parentId == null)
                ? categoryService.getActiveRootCategories()
                : categoryService.getActiveSubcategories(parentId);

        if (categories.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        for (int i = 0; i < categories.size(); i += 2) {
            var cat1 = categories.get(i);
            InlineKeyboardButton button1 = new InlineKeyboardButton(cat1.getName())
                    .callbackData("category_" + cat1.getId());

            if (i + 1 < categories.size()) {
                var cat2 = categories.get(i + 1);
                InlineKeyboardButton button2 = new InlineKeyboardButton(cat2.getName())
                        .callbackData("category_" + cat2.getId());
                keyboardMarkup.addRow(button1, button2);
            } else {
                keyboardMarkup.addRow(button1);
            }
        }

        if (parentId != null) {
            var currentCategory = categoryService.getCategoryWithParent(parentId);
            addBackButton(keyboardMarkup, currentCategory);
        }

        return keyboardMarkup;
    }

    /**
     * Метод добавляет кнопку "Назад" в Inline-клавиатуру в зависимости от текущей позиции в иерархии категорий
     * @param keyboardMarkup клавиатура для добавления кнопки
     * @param currentCategory текущая категория (уже загруженная с parent)
     */
    public static void addBackButton(InlineKeyboardMarkup keyboardMarkup, Category currentCategory) {
        log.info("[BACK BUTTON] Current category: {}, Parent ID: {}",
                currentCategory != null ? currentCategory.getName() : "null",
                currentCategory != null ? currentCategory.getParentId() : "null");

        if (currentCategory == null) {
            return;
        }

        Long parentId = currentCategory.getParentId();

        InlineKeyboardButton backButton;
        if (parentId != null) {
            backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("category_" + parentId);
        } else {
            backButton = new InlineKeyboardButton("↩️ Главное меню")
                    .callbackData("category_null");
        }
        keyboardMarkup.addRow(backButton);
    }

    /**
     * Создать клавиатуру с товарами для указанной категории
     */
    public InlineKeyboardMarkup getProductsWithQuantityKeyboard(Long categoryId) {
        var products = productService.getAvailableProductsWithStock(categoryId);

        if (products.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (var product : products) {
            if (product.getCount() == null || product.getCount() <= 0) {
                continue;
            }

            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId() + "_" + categoryId);

            keyboard.addRow(button);
        }

        var category = categoryService.getCategoryById(categoryId);
        if (category != null && category.getParentId() != null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("category_" + category.getParentId());
            keyboard.addRow(backButton);
        }

        return keyboard;
    }

    /**
     * Создать основную Reply-клавиатуру приложения
     * Содержит основные команды: меню, корзина, заказы, настройки
     */
    public ReplyKeyboardMarkup getMainMenuKeyboard() {
        KeyboardButton menuButton = new KeyboardButton(Messages.MENU);
        KeyboardButton cartButton = new KeyboardButton("🛒 Корзина");
        KeyboardButton ordersButton = new KeyboardButton("📦 Мои заказы");
        KeyboardButton settingsButton = new KeyboardButton("⚙️ Настройки");

        return new ReplyKeyboardMarkup(
                new KeyboardButton[]{menuButton, cartButton},
                new KeyboardButton[]{ordersButton, settingsButton}
        )
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true);
    }

    public InlineKeyboardMarkup createBasketKeyboard(Long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        if (cartService.isCartEmpty(chatId)) {
            InlineKeyboardButton menuButton = new InlineKeyboardButton("🍽️ Перейти в меню")
                    .callbackData("category_null");
            keyboard.addRow(menuButton);
        } else {
            InlineKeyboardButton deleteOneMoreButton = new InlineKeyboardButton("🗑️ Удалить товар")
                    .callbackData("cart_delete_one");
            keyboard.addRow(deleteOneMoreButton);

            InlineKeyboardButton clearButton = new InlineKeyboardButton("🗑️ Очистить корзину")
                    .callbackData("cart_clear");
            InlineKeyboardButton orderButton = new InlineKeyboardButton("📝 Оформить заказ")
                    .callbackData("order_create");
            keyboard.addRow(clearButton, orderButton);

            InlineKeyboardButton continueShoppingButton = new InlineKeyboardButton("🛒 Продолжить покупки")
                    .callbackData("category_null");
            keyboard.addRow(continueShoppingButton);
        }

        return keyboard;
    }
}

