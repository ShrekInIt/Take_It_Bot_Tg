package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminCategoryDto;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления категориями в админ-панели.
 * <p>
 * Доступен только пользователям с ролью SUPER_ADMIN.
 * Позволяет:
 *  - создавать категории
 *  - получать список всех категорий
 *  - обновлять существующие категории
 *  - удалять категории
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminCategoriesController {

    /**
     * Сервис с бизнес-логикой по работе с категориями
     */
    private final CategoryService categoryService;

    /**
     * Возвращает список всех категорий в системе
     *
     * @return список категорий
     */
    @GetMapping("/categories")
    public ResponseEntity<List<AdminCategoryDto>> getAll() {
        return ResponseEntity.ok(categoryService.findAll().stream().map(OrderMapper::toDtoCategory).toList());
    }

    /**
     * Получить категорию по id
     *
     * @param id идентификатор категории
     * @return HTTP 200 OK после удаления
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<AdminCategoryDto> getById(@PathVariable Long id) {
        Category c = categoryService.getById(id);
        return ResponseEntity.ok(OrderMapper.toDtoCategory(c));
    }

    /**
     * Поиск категории по названию
     *
     * @param name название категории
     * @return HTTP 200 OK после удаления
     */
    @GetMapping("/categories/search")
    public ResponseEntity<List<AdminCategoryDto>> search(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                categoryService.searchByName(name)
                        .stream()
                        .map(OrderMapper::toDtoCategory)
                        .toList()
        );
    }

    /**
     * Создаёт новую категорию
     *
     * @param req данные категории (название, описание и т.п.)
     * @return HTTP 200 OK при успешном создании
     */
    @PostMapping("/categories")
    public ResponseEntity<AdminCategoryDto> create(@RequestBody Map<String,Object> req) {
        Category created = categoryService.create(req);
        return ResponseEntity.ok(OrderMapper.toDtoCategory(created));
    }

    /**
     * Обновляет данные категории по ID
     *
     * @param id      идентификатор категории
     * @param req новые данные категории
     * @return обновлённая категория
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<AdminCategoryDto> update(@PathVariable Long id, @RequestBody Map<String,Object> req) {
        Category updated = categoryService.update(id, req);
        return ResponseEntity.ok(OrderMapper.toDtoCategory(updated));
    }

    /**
     * Удаляет категорию по ID
     *
     * @param id идентификатор категории
     * @return HTTP 200 OK после удаления
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
