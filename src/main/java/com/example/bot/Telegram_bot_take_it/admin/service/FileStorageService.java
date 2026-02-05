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

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Getter
    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

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

    public void deleteByUrl(String url) throws IOException {
        if (!url.startsWith("/uploads/")) return;

        Path path = root.resolve(url.replace("/uploads/", ""));
        Files.deleteIfExists(path);
    }

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
