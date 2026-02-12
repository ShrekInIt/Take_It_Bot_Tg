package com.example.bot.Telegram_bot_take_it.utils;

import com.example.bot.Telegram_bot_take_it.entity.Cart;
import com.example.bot.Telegram_bot_take_it.entity.CartItem;

import java.util.List;

public record  CartSnapshot(Cart cart, List<CartItem> items, int total) {
}
