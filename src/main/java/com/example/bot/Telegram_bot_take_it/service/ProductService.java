package com.example.bot.Telegram_bot_take_it.service;

import com.example.bot.Telegram_bot_take_it.entity.Product;
import com.example.bot.Telegram_bot_take_it.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    /**
     * Получить доступные продукты в категории с количеством > 0
     */
    public List<Product> getAvailableProductsWithStock(Long categoryId) {
        return productRepository.findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(categoryId);
    }

    /**
     * Проверить, есть ли доступные продукты с количеством > 0 в категории
     */
    public boolean hasAvailableProductsInCategory(Long categoryId) {
        return !productRepository.findByCategoryIdAndAvailableTrueAndCountGreaterThanZero(categoryId).isEmpty();
    }

    /**
     * Получить продукт по ID
     */
    @Transactional
    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    /**
     * Получить доступные сиропы
     */
    public List<Product> getAvailableSyrups() {
        Long syrupCategoryId = 20L;
        return getAvailableProductsWithStock(syrupCategoryId);
    }

    /**
     * Получить доступное альтернативное молоко
     */
    public List<Product> getAvailableMilks() {
        Long milkCategoryId = 21L;
        return getAvailableProductsWithStock(milkCategoryId);
    }

    /**
     * Проверить, является ли товар кофе
     */
    @Transactional(readOnly = true)
    public boolean isCoffeeProduct(Long productId) {
        try {
            Product product = getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
            return product.getCategory().getId() == 3L;
        } catch (Exception e) {
            log.error("Ошибка при проверке типа товара: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Сохранить/обновить продукт
     */
    @Transactional
    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    public long countAvailableProducts() {
        return productRepository.countByAvailable(true);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id = " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> searchByName(String name) {
        // Возвращаем все, у которых в имени содержится запрос (case-insensitive)
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Создать продукт.
     * request ожидает ключи: name, price, available, description, categoryId (опционально), count (опционально)
     */
    @Transactional
    public Product create(Map<String, Object> request) throws BadRequestException {
        Object nameObj = request.get("name");
        if (nameObj == null || nameObj.toString().isBlank()) {
            throw new BadRequestException("name is required");
        }
        Object catObj = request.get("categoryId");
        if (catObj == null || catObj.toString().isBlank()) {
            throw new BadRequestException("categoryId is required");
        }

        Product product = new Product();
        applyFieldsFromMap(product, request);

        if (product.getCategory().getId() == null) {
            throw new BadRequestException("categoryId must be a number and not null");
        }

        return productRepository.save(product);
    }

    /**
     * Обновить продукт по id.
     * Request содержит только поля, которые нужно обновить.
     */
    @Transactional
    public Product update(Long id, Map<String, Object> request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id = " + id));

        applyFieldsFromMap(product, request);

        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id = " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Универсальный маппер из Map -> Product (для create/update)
     * Поддерживает как поле categoryId (Long), так и связь category (Category).
     */
    private void applyFieldsFromMap(Product product, Map<String, Object> request) {
        if (request.containsKey("name")) {
            product.setName((String) request.get("name"));
        }

        if (request.containsKey("amount")) {
            Object amountObj = request.get("amount");
            Integer amount = null;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else if (amountObj != null) {
                try {
                    amount = Integer.parseInt(amountObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("Не удалось распарсить amount: {}", amountObj);
                }
            }
            if (amount != null) product.setAmount(amount);
        }

        if (request.containsKey("size")) {
            product.setSize((String) request.get("size"));
        }

        if (request.containsKey("available")) {
            Object avail = request.get("available");
            if (avail instanceof Boolean) product.setAvailable((Boolean) avail);
            else if (avail != null) product.setAvailable(Boolean.parseBoolean(avail.toString()));
        }

        if (request.containsKey("description")) {
            product.setDescription((String) request.get("description"));
        }

        if (request.containsKey("count")) {
            Object countObj = request.get("count");
            Integer count = null;
            if (countObj instanceof Number) count = ((Number) countObj).intValue();
            else if (countObj != null) {
                try { count = Integer.parseInt(countObj.toString()); }
                catch (NumberFormatException e) { log.warn("Не парсится count: {}", countObj); }
            }
            if (count != null) product.setCount(count);
        }

        if (request.containsKey("photo")) {
            product.setPhoto((String) request.get("photo"));
        }

        if (request.containsKey("categoryId")) {
            Object catObj = request.get("categoryId");
            Long catIdTemp = null;

            if (catObj instanceof Number) {
                catIdTemp = ((Number) catObj).longValue();
            } else if (catObj != null && !catObj.toString().isBlank()) {
                try {
                    catIdTemp = Long.parseLong(catObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("Не парсится categoryId: {}", catObj);
                }
            }

            final Long catId = catIdTemp; // вот теперь она effectively final

            if (catId != null) {
                categoryService.findById(catId).ifPresentOrElse(
                        product::setCategory,
                        () -> log.warn("Категория с id={} не найдена", catId)
                );
            } else {
                product.setCategory(null);
            }
        }

    }

    @Transactional
    public Product setPhoto(Long productId, String photoUrl) {
        Product p = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        p.setPhoto(photoUrl);


        return productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public boolean isImageFolderUsed(String folder) {
        String prefix = "products/" + folder + "/";
        return productRepository.existsProductWithPhotoInFolder(prefix);
    }

}
