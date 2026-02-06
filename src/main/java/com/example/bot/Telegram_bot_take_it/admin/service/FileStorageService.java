package com.example.bot.Telegram_bot_take_it.admin.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Сервис для хранения файлов на файловой системе (локальное хранилище).
 * <p>
 * Используется админкой (например, для изображений товаров):
 *  - инициализация корневой папки uploadDir
 *  - получение списка папок/файлов внутри "scope"
 *  - загрузка файла в папку (создаёт уникальное имя)
 *  - удаление файла по URL
 *  - удаление папки рекурсивно (со всем содержимым)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    /** Путь к корневой папке uploads из конфигурации (file.upload-dir) */
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Нормализованный абсолютный путь к корню загрузок.
     * Инициализируется в init().
     */
    @Getter
    private Path root;

    /**
     * Инициализирует корневую папку для загрузок.
     * <p>
     * Логика:
     *  - берёт uploadDir из конфигурации
     *  - приводит к абсолютному пути
     *  - создаёт директории, если их ещё нет
     */
    @PostConstruct
    void init() throws IOException {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /**
     * Возвращает список подпапок внутри scope (например, products/).
     *
     * @param scope область хранения (например "products")
     * @return список названий папок; если base-папка не существует — пустой список
     */
    public List<String> listFolders(String scope) throws IOException {
        Path base = root.resolve(scope);
        if (!Files.exists(base)) {
            log.error("❌ PATH NOT EXISTS");
            return List.of();
        }

        try (var s = Files.list(base)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    /**
     * Возвращает список файлов внутри папки scope/folder.
     *
     * @param scope  область хранения (например "products")
     * @param folder конкретная папка внутри scope
     * @return список имён файлов; если папки нет — пустой список
     */
    public List<String> listFiles(String scope, String folder) throws IOException {
        Path dir = root.resolve(scope).resolve(folder);
        if (!Files.exists(dir)) return List.of();

        try (var s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    /**
     * Загружает файл в папку scope/folder и возвращает публичный URL.
     * <p>
     * Логика:
     *  - проверяет, что файл не пустой
     *  - берёт расширение из originalFilename (если оно есть)
     *  - генерирует уникальное имя UUID + ext
     *  - создаёт директории scope/folder (если нет)
     *  - копирует поток файла в target (с перезаписью, если вдруг совпало)
     *  - возвращает URL формата /uploads/{scope}/{folder}/{name}
     *
     * @param file   загружаемый файл
     * @param scope  область хранения (например "products")
     * @param folder папка внутри scope
     * @return URL загруженного файла
     */
    public String upload(MultipartFile file, String scope, String folder) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Пустой файл");

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse("");

        String name = UUID.randomUUID() + ext;

        Path dir = root.resolve(scope).resolve(folder);
        Files.createDirectories(dir);

        Path target = dir.resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + scope + "/" + folder + "/" + name;
    }

    /**
     * Удаляет файл по URL (если это URL из /uploads/).
     * <p>
     * Логика:
     *  - игнорирует url, который не начинается с "/uploads/"
     *  - преобразует URL в относительный путь и удаляет файл, если он существует
     *
     * @param url URL файла (например "/uploads/products/x/y.jpg")
     */
    public void deleteByUrl(String url) throws IOException {
        if (!url.startsWith("/uploads/")) return;

        Path path = root.resolve(url.replace("/uploads/", ""));
        Files.deleteIfExists(path);
    }

    /**
     * Удаляет папку scope/folder рекурсивно со всем содержимым.
     * <p>
     * Логика:
     *  - если папки нет — ничего не делает
     *  - Files.walk(dir) проходит по всем вложенным файлам/папкам
     *  - сортировка reverseOrder нужна, чтобы сначала удалить файлы/вложенные папки,
     *    и только потом удалить корневую папку
     *
     * @param scope  область хранения
     * @param folder папка внутри scope
     */
    public void deleteFolder(String scope, String folder) throws IOException {
        Path dir = root.resolve(scope).resolve(folder);
        if (!Files.exists(dir)) return;

        try (Stream<Path> paths = Files.walk(dir)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
