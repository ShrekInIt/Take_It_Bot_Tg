package com.example.bot.Telegram_bot_take_it.admin.controller.adminController;

import com.example.bot.Telegram_bot_take_it.admin.dto.AdminProductDto;
import com.example.bot.Telegram_bot_take_it.admin.service.FileStorageService;
import com.example.bot.Telegram_bot_take_it.admin.utils.OrderMapper;
import com.example.bot.Telegram_bot_take_it.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * REST-контроллер админ-панели для управления изображениями товаров.
 * <p>
 * Доступен ролям ADMIN и SUPER_ADMIN.
 * Даёт возможность:
 *  - просматривать папки с изображениями товаров
 *  - просматривать файлы в конкретной папке
 *  - загружать изображение в папку и получать URL
 *  - удалять изображение по его URL
 *  - удалять папку (если она не используется товарами)
 *  - назначать товару основное фото (photo)
 *  - создавать папку для изображений
 */
@RestController
@RequestMapping("/api/admin/products/images")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminProductImageController {

    /**
     * Сервис с бизнес-логикой работы с товарами:
     * получение, поиск, создание, обновление и удаление.
     */
    private final ProductService productService;

    /**
     * Сервис с бизнес-логикой работы с файлами:
     * получение, поиск, создание, обновление и удаление.
     */
    private final FileStorageService fileStorageService;

    /**
     * Имя области (подкаталога) для хранения файлов товаров.
     * Используется как корневая папка внутри uploads/storage.
     */
    private static final String SCOPE = "products";

    /**
     * Возвращает список папок (каталогов) внутри области SCOPE.
     * <p>
     * Используется в админке, чтобы показать доступные папки с изображениями товаров.
     *
     * @return список названий папок
     */
    @GetMapping("/folders")
    public List<String> folders() throws IOException {
        return fileStorageService.listFolders(SCOPE);
    }

    /**
     * Возвращает список файлов (изображений) внутри указанной папки.
     *
     * @param folder имя папки внутри SCOPE (например "shoes", "bags" и т.п.)
     * @return список путей/имен файлов (в формате, который отдаёт FileStorageService)
     */
    @GetMapping
    public List<String> images(@RequestParam String folder) throws IOException {
        return fileStorageService.listFiles(SCOPE, folder);
    }

    /**
     * Загружает изображение в указанную папку и возвращает ссылку (url) на загруженный файл.
     *
     * @param file   файл изображения, загружаемый через multipart/form-data
     * @param folder папка внутри SCOPE, куда нужно сохранить файл
     * @return JSON вида {"url": "..."} с URL загруженного изображения
     */
    @PostMapping("/upload")
    public Map<String, String> upload(
            @RequestParam MultipartFile file,
            @RequestParam String folder
    ) throws IOException {
        String url = fileStorageService.upload(file, SCOPE, folder);
        return Map.of("url", url);
    }

    /**
     * Удаляет изображение по URL.
     *
     * @param url ссылка на файл (в формате, который понимает FileStorageService)
     */
    @DeleteMapping
    public void delete(@RequestParam String url) throws IOException {
        fileStorageService.deleteByUrl(url);
    }

    /**
     * Удаляет папку с изображениями (внутри SCOPE), но только если папка не используется ни одним товаром.
     * <p>
     * Перед удалением проверяет через productService.isImageFolderUsed(folder),
     * привязана ли эта папка к какому-либо продукту.
     *
     * @param folder имя папки для удаления
     * @return 204 No Content при успешном удалении или 400 Bad Request, если папка используется товарами
     */
    @DeleteMapping("/folders")
    public ResponseEntity<?> deleteFolder(@RequestParam String folder) throws IOException {

        if (productService.isImageFolderUsed(folder)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message", "Нельзя удалить папку: она используется продуктом"
                    ));
        }

        fileStorageService.deleteFolder(SCOPE, folder);
        return ResponseEntity.noContent().build();
    }

    /**
     * Устанавливает основное фото (photo) для товара по его ID.
     * <p>
     * Ожидает JSON в теле запроса с ключом "photo", например: {"photo": "uploads/products/folder/file.jpg"}.
     * Перед сохранением нормализует строку:
     *  - если начинается с "/" — убирает ведущий слэш
     *  - если начинается с "uploads/" — убирает этот префикс
     * <p>
     * Затем передаёт нормализованный путь в productService.setPhoto(...)
     * и возвращает обновлённый товар в формате AdminProductDto.
     *
     * @param id   ID товара
     * @param body JSON с ключом "photo"
     * @return товар после установки фото в формате AdminProductDto
     */
    @PutMapping("/{id}/photo")
    public AdminProductDto setPhoto(@PathVariable Long id, @RequestBody Map<String,String> body) {
        String photoUrl = body.get("photo");
        if (photoUrl != null) {
            if (photoUrl.startsWith("/")) photoUrl = photoUrl.substring(1);
            if (photoUrl.startsWith("uploads/")) photoUrl = photoUrl.substring("uploads/".length());
        }
        return OrderMapper.toDtoProduct((productService.setPhoto(id, photoUrl)));
    }

    /**
     * Создаёт папку для изображений товаров внутри SCOPE.
     * <p>
     * Формирует путь: root / SCOPE / folder и создаёт директории (если их нет).
     *
     * @param folder имя создаваемой папки
     * @return имя созданной папки (то же значение, что пришло в параметре folder)
     */
    @PostMapping("/folders")
    public String createFolder(@RequestParam String folder) throws IOException {
        Path dir = fileStorageService.getRoot().resolve(SCOPE).resolve(folder);
        Files.createDirectories(dir);
        return folder;
    }
}
