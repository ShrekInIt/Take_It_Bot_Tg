package com.example.bot.Telegram_bot_take_it.admin.controller;

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

@RestController
@RequestMapping("/api/admin/products/images")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminProductImageController {
    private final FileStorageService storage;
    private final ProductService productService;
    private final FileStorageService fileStorageService;

    private static final String SCOPE = "products";

    @GetMapping("/folders")
    public List<String> folders() throws IOException {
        return storage.listFolders(SCOPE);
    }

    @GetMapping
    public List<String> images(@RequestParam String folder) throws IOException {
        return storage.listFiles(SCOPE, folder);
    }

    @PostMapping("/upload")
    public Map<String, String> upload(
            @RequestParam MultipartFile file,
            @RequestParam String folder
    ) throws IOException {
        String url = storage.upload(file, SCOPE, folder);
        return Map.of("url", url);
    }

    @DeleteMapping
    public void delete(@RequestParam String url) throws IOException {
        storage.deleteByUrl(url);
    }

    @DeleteMapping("/folders")
    public ResponseEntity<?> deleteFolder(@RequestParam String folder) throws IOException {

        if (productService.isImageFolderUsed(folder)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message", "Нельзя удалить папку: она используется продуктом"
                    ));
        }

        storage.deleteFolder(SCOPE, folder);
        return ResponseEntity.noContent().build();
    }



    @PutMapping("/{id}/photo")
    public AdminProductDto setPhoto(@PathVariable Long id, @RequestBody Map<String,String> body) {
        String photoUrl = body.get("photo");
        if (photoUrl != null) {
            if (photoUrl.startsWith("/")) photoUrl = photoUrl.substring(1);
            if (photoUrl.startsWith("uploads/")) photoUrl = photoUrl.substring("uploads/".length());
        }
        return OrderMapper.toDtoProduct((productService.setPhoto(id, photoUrl)));
    }

    @PostMapping("/folders")
    public String createFolder(@RequestParam String folder) throws IOException {
        Path dir = fileStorageService.getRoot().resolve(SCOPE).resolve(folder);
        Files.createDirectories(dir);
        return folder;
    }
}
