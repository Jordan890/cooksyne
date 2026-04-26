package com.cooksyne.selfhosted.controller;

import com.cooksyne.core.domain.User;
import com.cooksyne.selfhosted.security.CurrentUserProvider;
import com.cooksyne.selfhosted.service.ImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final CurrentUserProvider currentUserProvider;
    private final ImageStorageService imageStorageService;

    public ImageController(CurrentUserProvider currentUserProvider, ImageStorageService imageStorageService) {
        this.currentUserProvider = currentUserProvider;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile image) throws IOException {

        User currentUser = currentUserProvider.getCurrentUser(jwt);
        String userId = String.valueOf(currentUser.getId());

        String originalFilename = image.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // Only allow image extensions
        String extLower = extension.toLowerCase();
        if (!extLower.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only image files (jpg, jpeg, png, gif, webp) are allowed."));
        }

        // Store in a per-user subdirectory
        Path storagePath = imageStorageService.getStoragePath();
        Path userDir = storagePath.resolve(userId).normalize();
        if (!userDir.startsWith(storagePath)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user path."));
        }
        Files.createDirectories(userDir);

        String filename = UUID.randomUUID() + extLower;
        Path target = userDir.resolve(filename).normalize();

        if (!target.startsWith(userDir)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file path."));
        }

        Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = "/images/" + userId + "/" + filename;
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/{userId}/{filename}")
    public ResponseEntity<Resource> serve(
            @PathVariable String userId,
            @PathVariable String filename) throws MalformedURLException {

        // Sanitize userId: only digits
        if (!userId.matches("\\d+")) {
            return ResponseEntity.badRequest().build();
        }

        // Sanitize filename: only UUID-style filenames with image extensions
        if (!filename.matches("[a-f0-9\\-]+\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.badRequest().build();
        }

        Path storagePath = imageStorageService.getStoragePath();
        Path file = storagePath.resolve(userId).resolve(filename).normalize();

        if (!file.startsWith(storagePath) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(file.toUri());

        String contentType = "image/jpeg";
        if (filename.endsWith(".png"))
            contentType = "image/png";
        else if (filename.endsWith(".gif"))
            contentType = "image/gif";
        else if (filename.endsWith(".webp"))
            contentType = "image/webp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}
