package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminProductDto;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
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
public class AdminProductController {
    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<List<AdminProductDto>> getAllProducts() {
        return ResponseEntity.ok(
                productService.findAll().stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> getProductById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(toDto(product));
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<AdminProductDto>> searchProducts(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                productService.searchByName(name).stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @PostMapping("/products")
    public ResponseEntity<AdminProductDto> createProduct(
            @RequestBody Map<String, Object> request
    ) throws BadRequestException {
        Product product = productService.create(request);
        return ResponseEntity.ok(toDto(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Product updated = productService.update(id, request);
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminProductDto toDto(Product p) {
        return AdminProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .amount(p.getAmount())
                .available(p.getAvailable())
                .description(p.getDescription())
                .photo(p.getPhoto())
                .size(p.getSize())
                .count(p.getCount())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .build();
    }
}
