package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

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
     * Создаёт новый товар.
     * <p>
     * Принимает Map с данными товара (поля зависят от реализации productService.create),
     * создаёт Product и возвращает созданный товар в формате AdminProductDto.
     *
     * @param request данные для создания товара (ключи/значения из JSON тела запроса)
     * @return созданный товар в формате AdminProductDto
     * @throws BadRequestException если входные данные некорректны (валидация на стороне сервиса)
     */
    @PostMapping("/products")
    public ResponseEntity<AdminProductDto> createProduct(
            @RequestBody Map<String, Object> request
    ) throws BadRequestException {
        Product product = productService.create(request);
        return ResponseEntity.ok(OrderMapper.toDtoProduct(product));
    }

    /**
     * Обновляет существующий товар по ID.
     * <p>
     * Принимает Map с обновляемыми полями, передаёт их в сервис,
     * и возвращает обновлённый товар в формате AdminProductDto.
     *
     * @param id      ID товара, который нужно обновить
     * @param request данные для обновления товара (ключи/значения из JSON тела запроса)
     * @return обновлённый товар в формате AdminProductDto
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<AdminProductDto> updateProduct(
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
