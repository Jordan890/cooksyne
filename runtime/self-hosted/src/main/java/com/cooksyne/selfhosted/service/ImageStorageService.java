package com.cooksyne.selfhosted.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageStorageService {

    @Value("${COOKSYNE_STORAGE_IMAGE_DIR:${cooksyne.storage.image-dir:#{systemProperties['user.home'] + '/.cooksyne/images'}}}")
    private String imageDir;

    private Path storagePath;

    @PostConstruct
    public void init() throws IOException {
        storagePath = Paths.get(imageDir).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Delete the image file for a given image URL (e.g. "/api/images/42/uuid.jpg").
     * Silently ignores null/blank URLs or missing files.
     */
    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank())
            return;

        // URL format: /api/images/{userId}/{filename}
        String prefix = "/api/images/";
        if (!imageUrl.startsWith(prefix))
            return;

        String relativePath = imageUrl.substring(prefix.length()); // "42/uuid.jpg"
        Path file = storagePath.resolve(relativePath).normalize();

        // Safety: ensure it's still inside our storage directory
        if (!file.startsWith(storagePath))
            return;

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Log but don't fail the recipe deletion
        }
    }
}
