package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.dto.CartItemGroupDTO;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.utils.Messages;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class KeyboardService {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CartService cartService;
    private final SyrupPriceService syrupPriceService;
    private final CartItemAddonService cartItemAddonService;

    private static final DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

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
    @Transactional(readOnly = true)
    public boolean needsAddons(Product product) {
        if (product == null) {
            return false;
        }

        Long categoryId = getProductCategoryIdSafely(product);
        boolean isCoffee = categoryService.isCoffeeCategoryById(categoryId);

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
     * Безопасный метод для получения categoryId из продукта
     * Работает как с attached, так и с detached продуктами
     */
    private Long getProductCategoryIdSafely(Product product) {
        try {
            return product.getCategoryId();
        } catch (LazyInitializationException e) {
            log.debug("Продукт detached, загружаем заново: {}", product.getId());
            Product freshProduct = productService.getProductById(product.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Продукт не найден"));
            return freshProduct.getCategoryId();
        }
    }

    /**
     * Создать клавиатуру для выбора добавок
     */
    public InlineKeyboardMarkup createAddonsKeyboard(Long productId, int quantity, Long categoryId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        Long syrupCategoryId = 20L;
        Long milkCategoryId = 21L;

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
     * Создание клавиатуры для удаления товара
     */
    public InlineKeyboardMarkup createDeleteProductKeyboard(Long firstCartItemId, int deleteQuantity, int maxQuantity) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton minusButton = new InlineKeyboardButton("➖")
                .callbackData("cart_delete_quantity_" + firstCartItemId + "_" + deleteQuantity + "_dec");

        InlineKeyboardButton quantityButton = new InlineKeyboardButton(deleteQuantity + " шт")
                .callbackData("no_action");

        InlineKeyboardButton plusButton = new InlineKeyboardButton("➕")
                .callbackData("cart_delete_quantity_" + firstCartItemId + "_" + deleteQuantity + "_inc");

        keyboard.addRow(minusButton, quantityButton, plusButton);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton("🗑️ Удалить " + deleteQuantity + " шт")
                .callbackData("cart_delete_confirm_" + firstCartItemId + "_" + deleteQuantity);
        keyboard.addRow(deleteButton);

        InlineKeyboardButton deleteAllButton = new InlineKeyboardButton("🗑️ Удалить полностью товар (" + maxQuantity + " шт)")
                .callbackData("cart_delete_all_" + firstCartItemId);
        keyboard.addRow(deleteAllButton);

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору товара")
                .callbackData("cart_delete_one");
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

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад в корзину")
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

        if (category != null && category.getParentId() == null) {
            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Главное меню")
                    .callbackData("category_null");
            keyboard.addRow(backButton);
        }
        System.out.println(category);
        return keyboard;
    }

    /**А
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

    /**
     * Создать InlineKeyboardMarkup клавиатуру для корзины
     */
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

    /**
     * Создать InlineKeyboardMarkup клавиатуру для сиропов и альт.молока
     */
    public InlineKeyboardMarkup createKeyboardSyrupAndMilk(Long cartItemId, long productId, int quantity,
                                                           long categoryId, String addon, Product syrup, Product milk) {
        InlineKeyboardMarkup keyboardMarkup;
        if(addon.equals("syrup")) {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить сироп");
            addSyrupButton.setCallbackData("addons_add_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);

            keyboardMarkup.addRow(addSyrupButton);


            if (syrup != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать сироп");
                removeSyrupButton.setCallbackData("addons_remove_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("cart_addon_syrup_" + productId + "_" + quantity + "_" + categoryId);
            keyboardMarkup.addRow(backButton);
        }else {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить альт. молоко");
            addSyrupButton.setCallbackData("addons_add_milk_" + cartItemId + "_" + productId + "_" + quantity + "_"  + categoryId);

            keyboardMarkup.addRow(addSyrupButton);


            if (milk != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать альт. молоко");
                removeSyrupButton.setCallbackData("addons_remove_milk_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("cart_addon_milk_" + productId + "_" + quantity + "_" + categoryId);
            keyboardMarkup.addRow(backButton);
        }

        return keyboardMarkup;
    }

    /**
     * Создать InlineKeyboardMarkup кнопки назад для добавок
     */
    public InlineKeyboardMarkup createButtonBackForAddonsMilkOrSyrup(Long cartItemId, long productId, int quantity,
                                                              long categoryId, String addon) {
        InlineKeyboardMarkup keyboard;

        if (addon.equals("milk")) {
            keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("addons_milk_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);
        }else {
            keyboard = new InlineKeyboardMarkup();

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("addons_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);
        }

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуры доступных добавок
     */
    public InlineKeyboardMarkup createKeyboardForSelectedMilkOrSyrup(Long cartItemId, long productId, int quantity,
                                                              long categoryId, String addon, Product mainProduct) {
        InlineKeyboardMarkup keyboard;

        if (addon.equals("milk")) {
            keyboard = new InlineKeyboardMarkup();
            List<Product> milks = productService.getAvailableMilks();

            if (milks.isEmpty()) {
                InlineKeyboardButton unavailableButton = new InlineKeyboardButton("⚠️ Альт.молоко временно недоступны")
                        .callbackData("noop");
                keyboard.addRow(unavailableButton);
            } else {
                for (Product milk : milks) {
                    String buttonText = String.format("%s +%d₽",
                            milk.getName(), milk.getAmount());

                    InlineKeyboardButton syrupButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_select_milk_" + cartItemId + "_" + milk.getId() + "_" + productId + "_" + quantity + "_" + categoryId);
                    keyboard.addRow(syrupButton);
                }
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("addons_milk_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);
        }else {
            keyboard = new InlineKeyboardMarkup();
            List<Product> syrups = productService.getAvailableSyrups();

            if (syrups.isEmpty()) {
                InlineKeyboardButton unavailableButton = new InlineKeyboardButton("⚠️ Сиропы временно недоступны")
                        .callbackData("noop");
                keyboard.addRow(unavailableButton);
            } else {
                for (Product syrup : syrups) {
                    int syrupPrice = syrupPriceService.calculateSyrupPriceForSize(syrup, mainProduct);
                    String buttonText = String.format("%s +%d₽",
                            syrup.getName(), syrupPrice);

                    InlineKeyboardButton syrupButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_select_syrup_" + cartItemId + "_" + syrup.getId() + "_" + productId + "_" + quantity + "_" + categoryId);
                    keyboard.addRow(syrupButton);
                }
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к добавкам")
                    .callbackData("addons_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);
            keyboard.addRow(backButton);
        }

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для действий с добавками
     */
    public InlineKeyboardMarkup createKeyboardForMilkActionOrSyrupAction(Long cartItemId, long productId, int quantity,
                                                            long categoryId, Product addonProduct, String addon){
        InlineKeyboardMarkup keyboardMarkup;
        if (addon.equals("milk")) {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить альт. молоко");
            addSyrupButton.setCallbackData("addons_add_milk_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);

            keyboardMarkup.addRow(addSyrupButton);

            if (addonProduct != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать альт. молоко");
                removeSyrupButton.setCallbackData("addons_remove_milk_" + cartItemId + "_" + productId + "_" + quantity + "_" + categoryId);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("cart_addon_milk_" + productId + "_" + quantity + "_" + categoryId);
            keyboardMarkup.addRow(backButton);
        }else {
            keyboardMarkup = new InlineKeyboardMarkup();

            InlineKeyboardButton addSyrupButton = new InlineKeyboardButton();
            addSyrupButton.setText("➕ Добавить сироп");
            addSyrupButton.setCallbackData("addons_add_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_"  + categoryId);

            keyboardMarkup.addRow(addSyrupButton);

            if (addonProduct != null) {
                InlineKeyboardButton removeSyrupButton = new InlineKeyboardButton();
                removeSyrupButton.setText("➖ Убрать сироп");
                removeSyrupButton.setCallbackData("addons_remove_syrup_" + cartItemId + "_" + productId + "_" + quantity + "_"  + categoryId);

                keyboardMarkup.addRow(removeSyrupButton);
            }

            InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                    .callbackData("cart_addon_syrup_" + productId + "_" + quantity + "_" + categoryId);
            keyboardMarkup.addRow(backButton);
        }
        return keyboardMarkup;
    }

    /**
     * Создать InlineKeyboardMarkup кнопку "Перейти в меню"
     */
    public InlineKeyboardMarkup createButtonGoMenu(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton menuButton = new InlineKeyboardButton("🍽️ Перейти в меню")
                .callbackData("category_null");
        keyboard.addRow(menuButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для меню
     */
    public InlineKeyboardMarkup createKeyboardMenu(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton basketButton = new InlineKeyboardButton("🛒 Перейти в корзину")
                .callbackData("cart_back");
        keyboard.addRow(basketButton);

        InlineKeyboardButton continueButton = new InlineKeyboardButton("🛍️ Продолжить покупки")
                .callbackData("category_null");
        keyboard.addRow(continueButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для продуктов и добавок
     */
    public InlineKeyboardMarkup createKeyboardAddAddonsInBasket(List<CartItem> cartItems, long productId, int quantity, long categoryId, String addon){
        InlineKeyboardMarkup keyboard;

        if(addon.equals("syrup")) {
            keyboard = new InlineKeyboardMarkup();
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                if (needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_syrup_" + cartItem.getId() + "_" + productId + "_" + quantity + "_" + categoryId);
                    keyboard.addRow(itemButton);
                }
            }
        }else {
            keyboard = new InlineKeyboardMarkup();
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();

                if (needsAddons(product)) {
                    String buttonText = String.format("%s - Добавки %s",
                            product.getName(),
                            (cartItemAddonService.hasAddons(cartItem.getId()) ? " ✅" : " ❌"));

                    InlineKeyboardButton itemButton = new InlineKeyboardButton(buttonText)
                            .callbackData("addons_milk_" + cartItem.getId() + "_" + productId + "_" + quantity + "_" + categoryId);
                    keyboard.addRow(itemButton);
                }
            }
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к выбору добавок")
                .callbackData("addons_show_" + productId + "_" + quantity + "_" + categoryId);
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для заказа
     */
    public InlineKeyboardMarkup createKeyboardForChoiceDelivery(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton pickupButton = new InlineKeyboardButton("🚶 Самовывоз")
                .callbackData("order_delivery_pickup");

        InlineKeyboardButton deliveryButton = new InlineKeyboardButton("🚚 Доставка")
                .callbackData("order_delivery_delivery");

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отменить заказ")
                .callbackData("order_cancel");

        keyboard.addRow(pickupButton, deliveryButton);
        keyboard.addRow(cancelButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup кнопку "Пропустить"
     */
    public InlineKeyboardMarkup createButtonSkip(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton skipButton = new InlineKeyboardButton("Пропустить")
                .callbackData("order_skip");

        keyboard.addRow(skipButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для подтверждения заказа
     */
    public InlineKeyboardMarkup createKeyboardConfirmOrder(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Подтвердить заказ")
                .callbackData("order_confirm");

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отменить заказ")
                .callbackData("order_cancel");

        keyboard.addRow(confirmButton);
        keyboard.addRow(cancelButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup кнопку "Вернуться в корзину"
     */
    public InlineKeyboardMarkup createButtonBackBasket(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton backButton = new InlineKeyboardButton("🛒 Вернуться в корзину")
                .callbackData("cart_back");

        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создание клавиатуры для истории заказов
     */
    public InlineKeyboardMarkup createOrderHistoryKeyboard(List<Order> orders) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Order value : orders) {
            String buttonText = String.format(
                    "%s %s №%s • %s₽",
                    getStatusEmoji(value.getStatus()),
                    value.getCreatedAt().format(DATE_FORMATTER_SHORT),
                    value.getOrderNumber().substring(Math.max(0, value.getOrderNumber().length() - 6)),
                    value.getTotalAmount()
            );
            InlineKeyboardButton detailsButton = new InlineKeyboardButton(buttonText)
                    .callbackData("order_details_" + value.getId());
            keyboard.addRow(detailsButton);
        }

        InlineKeyboardButton clearHistoryButton = new InlineKeyboardButton("🗑️ Очистить историю")
                .callbackData("order_clear_history");
        keyboard.addRow(clearHistoryButton);

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("main_menu");
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup клавиатуру для заказа
     */
    public InlineKeyboardMarkup createKeyboardForOrders(Long orderId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад к истории")
                .callbackData("order_history");

        InlineKeyboardButton repeatButton = new InlineKeyboardButton("🔄 Повторить заказ")
                .callbackData("repeat_order_" + orderId);

        keyboard.addRow(backButton, repeatButton);

        return keyboard;
    }

    /**
     * Создать InlineKeyboardMarkup кнопку назад в меню
     */
    public InlineKeyboardMarkup createButtonMainMenuBack(){
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton backButton = new InlineKeyboardButton("↩️ Назад")
                .callbackData("main_menu");
        keyboard.addRow(backButton);

        return keyboard;
    }

    /**
     * Создать комбинированную клавиатуру (подкатегории + товары)
     * @Transactional для безопасного доступа к полям категории
     */
    @Transactional(readOnly = true)
    public InlineKeyboardMarkup createCombinedKeyboard(Long categoryId, List<Category> subcategories, List<com.example.bot.Telegram_bot_take_it.entity.Product> products) {
        log.info("=== CREATE COMBINED KEYBOARD ===");
        log.info("Category ID: {}", categoryId);
        log.info("Number of subcategories passed: {}", subcategories.size());
        log.info("Number of products passed: {}", products.size());
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        for (Category subcategory : subcategories) {
            InlineKeyboardButton button = new InlineKeyboardButton("📁 " + subcategory.getName())
                    .callbackData("category_" + subcategory.getId());
            keyboard.addRow(button);
        }

        for (com.example.bot.Telegram_bot_take_it.entity.Product product : products) {
            String buttonText = String.format("%s - %d₽",
                    product.getName(),
                    product.getAmount());

            InlineKeyboardButton button = new InlineKeyboardButton(buttonText)
                    .callbackData("product_" + product.getId() + "_" + categoryId);

            keyboard.addRow(button);
        }

        Category category = categoryService.getCategoryWithParent(categoryId);
        if (category != null) {
            KeyboardService.addBackButton(keyboard, category);
        }

        return keyboard;
    }

    /**
     * Получить эмодзи для статуса заказа
     */
    public String getStatusEmoji(Order.OrderStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case CONFIRMED -> "✅";
            case PREPARING -> "👨‍🍳";
            case READY -> "🚀";
            case DELIVERING -> "🚚";
            case COMPLETED -> "🎉";
            case CANCELLED -> "❌";
        };
    }
}

