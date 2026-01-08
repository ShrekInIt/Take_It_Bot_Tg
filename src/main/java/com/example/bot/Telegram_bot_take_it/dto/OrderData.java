package com.example.bot.Telegram_bot_take_it.dto;

import com.example.bot.Telegram_bot_take_it.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderData {
    private Long chatId;
    private String deliveryType;
    private String phoneNumber;
    private String address;
    private String comments;
    private List<CartItem> cartItems;
}
