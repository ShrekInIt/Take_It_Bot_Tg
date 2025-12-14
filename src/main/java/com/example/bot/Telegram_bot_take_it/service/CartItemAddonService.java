package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import com.example.bot.Telegram_bot_take_it.entity.CartItemAddon;
import com.example.bot.Telegram_bot_take_it.repository.CartItemAddonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemAddonService {
    private final CartItemAddonRepository cartItemAddonRepository;

    public List<CartItemAddon> getAddonsForCartItem(CartItem cartItem) {
        return cartItemAddonRepository.findByCartItem(cartItem);
    }
}
