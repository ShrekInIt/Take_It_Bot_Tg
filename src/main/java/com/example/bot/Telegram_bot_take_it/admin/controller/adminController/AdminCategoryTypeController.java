package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

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


/**
 * Контроллер для управления типами категорий в админ-панели.
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Используется для:
 *  - просмотра всех типов категорий
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryTypeController {

    /**
     * Сервис с бизнес-логикой для работы с типами категорий
     */
    private final CategoryTypeRepository categoryTypeRepository;

    /**
     * Возвращает список всех типов категорий
     *
     * @return список типов категорий
     */
    @GetMapping("/category-types")
    public ResponseEntity<List<CategoryType>> getAll() {
        return ResponseEntity.ok(categoryTypeRepository.findAll());
    }
}
