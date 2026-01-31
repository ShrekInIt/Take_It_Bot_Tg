package com.example.bot.Telegram_bot_take_it.admin.controller;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminProductDto;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
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
                        .map(OrderMapper::toDtoProduct)
                        .toList()
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> getProductById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<AdminProductDto>> searchProducts(
            @RequestParam String name
    ) {
        return ResponseEntity.ok(
                productService.searchByName(name).stream()
                        .map(OrderMapper::toDtoProduct)
                        .toList()
        );
    }

    @PostMapping("/products")
    public ResponseEntity<AdminProductDto> createProduct(
            @RequestBody Map<String, Object> request
    ) throws BadRequestException {
        Product product = productService.create(request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Product updated = productService.update(id, request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(updated));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
