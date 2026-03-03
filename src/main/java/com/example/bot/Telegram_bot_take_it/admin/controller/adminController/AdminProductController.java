package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminProductDto;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.dto.request.CreateProductRequest;
import com.example.bot.Telegram_bot_take_it.dto.request.UpdateProductRequest;
import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер админ-панели для управления товарами (Product).
 * <p>
 * Доступ разрешён ролям ADMIN и SUPER_ADMIN.
 * Предоставляет CRUD-операции и поиск по названию.
 * Все ответы возвращаются в формате AdminProductDto (через OrderMapper).
 */
@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    /**
     * Сервис с бизнес-логикой работы с товарами:
     * получение, поиск, создание, обновление и удаление.
     */
    private final ProductService productService;

    /**
     * Возвращает список всех товаров.
     * <p>
     * Берёт все Product из сервиса и преобразует каждый в AdminProductDto,
     * чтобы отдать данные в удобном для админки виде.
     *
     * @return список товаров в формате AdminProductDto
     */
    @GetMapping("/products")
    public ResponseEntity<List<AdminProductDto>> getAllProducts() {
        return ResponseEntity.ok(
                productService.findAll().stream()
                        .map(OrderMapper::toDtoProduct)
                        .toList()
        );
    }

    /**
     * Возвращает товар по его идентификатору.
     *
     * @param id ID товара
     * @return товар в формате AdminProductDto
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> getProductById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    /**
     * Ищет товары по названию (по параметру name).
     * <p>
     * Используется для поиска в админке: возвращает список совпадающих товаров,
     * преобразованных в AdminProductDto.
     *
     * @param name строка для поиска по названию товара
     * @return список найденных товаров в формате AdminProductDto
     */
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

    /**
     * Создаёт новый товар с использованием DTO и валидации.
     * <p>
     * Принимает CreateProductRequest с валидированными данными товара,
     * создаёт Product и возвращает созданный товар в формате AdminProductDto.
     *
     * @param request данные для создания товара с валидацией
     * @return созданный товар в формате AdminProductDto
     */
    @PostMapping("/products")
    public ResponseEntity<AdminProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        Product product = productService.create(request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    /**
     * Создаёт новый товар (legacy метод для обратной совместимости).
     * @deprecated Используйте {@link #createProduct(CreateProductRequest)} вместо этого
     */
    @Deprecated
    @PostMapping("/products/legacy")
    public ResponseEntity<AdminProductDto> createProductLegacy(
            @RequestBody Map<String, Object> request
    ) throws BadRequestException {
        Product product = productService.create(request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    /**
     * Обновляет существующий товар по ID с использованием DTO и валидации.
     * <p>
     * Принимает UpdateProductRequest с валидированными обновляемыми полями,
     * передаёт их в сервис, и возвращает обновлённый товар в формате AdminProductDto.
     *
     * @param id      ID товара, который нужно обновить
     * @param request данные для обновления товара с валидацией
     * @return обновлённый товар в формате AdminProductDto
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        Product updated = productService.update(id, request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(updated));
    }

    /**
     * Обновляет существующий товар по ID (legacy метод для обратной совместимости).
     * @deprecated Используйте {@link #updateProduct(Long, UpdateProductRequest)} вместо этого
     */
    @Deprecated
    @PutMapping("/products/{id}/legacy")
    public ResponseEntity<AdminProductDto> updateProductLegacy(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        Product updated = productService.update(id, request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(updated));
    }

    /**
     * Удаляет товар по ID.
     * <p>
     * После успешного удаления возвращает 204 No Content.
     *
     * @param id ID товара, который нужно удалить
     * @return ответ без тела (204 No Content)
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
