package com.cartandcook.selfhosted.controller;

import com.cartandcook.core.api.AiService;
import com.cartandcook.core.api.OcrService;
import com.cartandcook.core.domain.RecipeAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

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
        long start = System.currentTimeMillis();
        log.info("analyze-food: generating ingredients for '{}'", title.trim());
        RecipeAnalysis result = aiService.analyzeFoodByTitle(title.trim());
        log.info("analyze-food: AI complete in {} ms", System.currentTimeMillis() - start);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/analyze-recipe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeAnalysis> analyzeRecipe(
            @RequestParam("image") MultipartFile image) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        long start = System.currentTimeMillis();
        log.info("analyze-recipe: starting OCR on {} bytes", image.getSize());
        String extractedText = ocrService.extractText(image.getBytes());
        long ocrDone = System.currentTimeMillis();
        log.info("analyze-recipe: OCR complete in {} ms, sending {} chars to AI",
                ocrDone - start, extractedText.length());
        log.info("analyze-recipe: OCR output:\n{}", extractedText);
        RecipeAnalysis result = aiService.analyzeRecipeByText(extractedText);
        log.info("analyze-recipe: AI complete in {} ms (total {} ms)",
                System.currentTimeMillis() - ocrDone, System.currentTimeMillis() - start);
        return ResponseEntity.ok(result);
    }
}
