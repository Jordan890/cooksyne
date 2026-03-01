package com.cartandcook.selfhosted.controller;

import com.cartandcook.core.ai.FoodClassifier;
import com.cartandcook.core.ai.TextExtractionResult;
import com.cartandcook.core.ai.TextExtractor;
import com.cartandcook.core.ai.VisionResult;
import com.cartandcook.core.domain.Recipe;
import com.cartandcook.core.domain.User;
import com.cartandcook.selfhosted.contracts.RecipeRequest;
import com.cartandcook.selfhosted.contracts.RecipeResponse;
import com.cartandcook.selfhosted.security.CurrentUserProvider;
import com.cartandcook.selfhosted.service.RecipeServiceSpring;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeServiceSpring recipeService;
    private final CurrentUserProvider currentUserProvider;
    private final FoodClassifier foodClassifier;
    private final TextExtractor textExtractor;

    public RecipeController(RecipeServiceSpring recipeService,
                            CurrentUserProvider currentUserProvider,
                            FoodClassifier foodClassifier, TextExtractor textExtractor) {
        this.recipeService = recipeService;
        this.currentUserProvider = currentUserProvider;
        this.foodClassifier = foodClassifier;
        this.textExtractor = textExtractor;
    }
    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes(@AuthenticationPrincipal Jwt jwt) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        List<RecipeResponse> response = recipeService.getAllRecipes(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        Recipe recipe = recipeService.getRecipeById(id, currentUser.getId());
        if (recipe == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(recipe));
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> upsertRecipe(@AuthenticationPrincipal Jwt jwt, @RequestBody RecipeRequest request) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        Recipe recipe = Recipe.hydrate(
                request.getId(),
                request.getName(),
                request.getCategory(),
                request.getDescription(),
                request.getIngredients(),
                currentUser.getId()
        );
        Recipe saved = recipeService.upsertRecipe(recipe);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public void deleteRecipe(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        recipeService.deleteRecipe(id, currentUser.getId());
    }

    @PostMapping("/classify")
    public VisionResult classify(@RequestParam MultipartFile file) throws IOException {
        return foodClassifier.classify(file.getBytes());
    }

    @PostMapping("/extract")
    public TextExtractionResult extract(@RequestParam MultipartFile file) throws IOException {
        return textExtractor.extract(file.getBytes());
    }

    // Mapper to Response DTO
    private RecipeResponse toResponse(Recipe recipe) {
        RecipeResponse response = new RecipeResponse();
        response.setId(recipe.getId() != null ? recipe.getId() : null);
        response.setName(recipe.getName());
        response.setCategory(recipe.getCategory());
        response.setDescription(recipe.getDescription());
        response.setIngredients(recipe.getIngredients());
        return response;
    }
}
