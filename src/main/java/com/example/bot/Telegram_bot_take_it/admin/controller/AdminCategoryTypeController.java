package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.entity.CategoryType;
import com.example.bot.Telegram_bot_take_it.repository.CategoryTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryTypeController {
    private final CategoryTypeRepository categoryTypeRepository;

    @GetMapping("/category-types")
    public ResponseEntity<List<CategoryType>> getAll() {
        return ResponseEntity.ok(categoryTypeRepository.findAll());
    }
}
