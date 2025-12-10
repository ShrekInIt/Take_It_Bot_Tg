package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.AddonType;
import com.example.bot.Telegram_bot_take_it.repository.AddonTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddonTypeService {

    private AddonTypeRepository repository;

    public List<AddonType> findAllAddonTypes() {
        return repository.findByCategoryIdAndAvailableTrueAndCountGreaterThanZero();
    }
}
