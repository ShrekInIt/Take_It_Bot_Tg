package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    /**
     * Разделить позицию в корзине при добавлении добавок
     * @param chatId ID чата
     * @param cartItemId ID исходной позиции
     * @param quantityToSplit количество для отделения (обычно 1)
     * @return новая позиция с добавками (null, если не удалось)
     */
    @Transactional
    public CartItem splitCartItemForAddons(Long chatId, Long cartItemId, int quantityToSplit) {
        try {
            log.info("Разделение позиции для добавок: chatId={}, cartItemId={}, quantity={}",
                    chatId, cartItemId, quantityToSplit);

            // 1. Получаем исходную позицию
            CartItem originalItem = cartService.getCartItemByIdWithUserCheck(chatId, cartItemId);
            if (originalItem == null) {
                throw new RuntimeException("Позиция не найдена");
            }

            int originalQuantity = originalItem.getCountProduct();

            if (originalQuantity < quantityToSplit) {
                throw new RuntimeException("Недостаточно товара в позиции");
            }

            // 2. Если нужно отделить все товары, просто возвращаем оригинальную позицию
            if (originalQuantity == quantityToSplit) {
                log.info("Отделяем всю позицию: {} шт.", originalQuantity);
                return originalItem;
            }

            // 3. Создаем новую позицию для товаров с добавками
            CartItem newItem = new CartItem();
            newItem.setCart(originalItem.getCart());
            newItem.setProduct(originalItem.getProduct());
            newItem.setCountProduct(quantityToSplit);

            // 4. Уменьшаем количество в исходной позиции
            originalItem.setCountProduct(originalQuantity - quantityToSplit);

            // 5. Сохраняем изменения
            cartItemRepository.save(originalItem);
            CartItem savedNewItem = cartItemRepository.save(newItem);

            log.info("Позиция разделена: исходная={} шт., новая={} шт.",
                    originalItem.getCountProduct(), savedNewItem.getCountProduct());

            return savedNewItem;

        } catch (Exception e) {
            log.error("Ошибка разделения позиции для добавок: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось разделить позицию для добавок");
        }
    }
}
