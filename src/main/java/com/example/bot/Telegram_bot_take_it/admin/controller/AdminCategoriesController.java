package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminCategoryDto;
import com.example.bot.Telegram_bot_take_it.entity.Category;
import com.example.bot.Telegram_bot_take_it.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoriesController {
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<List<AdminCategoryDto>> getAll() {
        return ResponseEntity.ok(categoryService.findAll().stream().map(this::toDto).toList());
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<AdminCategoryDto> getById(@PathVariable Long id) {
        Category c = categoryService.getById(id);
        return ResponseEntity.ok(toDto(c));
    }

    @GetMapping("/categories/search")
    public ResponseEntity<List<AdminCategoryDto>> search(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                categoryService.searchByName(name)
                        .stream()
                        .map(this::toDto)
                        .toList()
        );
    }


    @PostMapping("/categories")
    public ResponseEntity<AdminCategoryDto> create(@RequestBody Map<String,Object> req) {
        Category created = categoryService.create(req);
        return ResponseEntity.ok(toDto(created));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<AdminCategoryDto> update(@PathVariable Long id, @RequestBody Map<String,Object> req) {
        Category updated = categoryService.update(id, req);
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminCategoryDto toDto(Category c) {
        return AdminCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .categoryTypeId(c.getCategoryType() != null ? c.getCategoryType().getId() : null)
                .categoryTypeName(c.getCategoryType() != null ? c.getCategoryType().getName() : null)
                .build();
    }
}
