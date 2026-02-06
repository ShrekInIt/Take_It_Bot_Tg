package com.example.bot.Telegram_bot_take_it.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramPhotoIdCacheService {
    private final Map<String, String> fileIdCache = new ConcurrentHashMap<>();

    public String get(String key) {
        return fileIdCache.get(key);
    }

    public void put(String key, String fileId) {
        if (key == null || fileId == null || key.isBlank() || fileId.isBlank()) return;
        fileIdCache.put(key, fileId);
    }

    public void invalidate(String key) {
        if (key == null) return;
        fileIdCache.remove(key);
    }
}
