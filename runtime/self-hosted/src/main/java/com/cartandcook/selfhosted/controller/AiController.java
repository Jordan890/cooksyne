package com.cartandcook.selfhosted.controller;

import com.cartandcook.core.api.AiService;
import com.cartandcook.core.api.OcrService;
import com.cartandcook.core.domain.RecipeAnalysis;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final OcrService ocrService;

    public AiController(AiService aiService, OcrService ocrService) {
        this.aiService = aiService;
        this.ocrService = ocrService;
    }

    @PostMapping("/analyze-food")
    public ResponseEntity<RecipeAnalysis> analyzeFood(
            @RequestParam("title") String title) {

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        RecipeAnalysis result = aiService.analyzeFoodByTitle(title.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/analyze-recipe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeAnalysis> analyzeRecipe(
            @RequestParam("image") MultipartFile image) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String extractedText = ocrService.extractText(image.getBytes());
        RecipeAnalysis result = aiService.analyzeRecipeByText(extractedText);
        return ResponseEntity.ok(result);
    }
}
