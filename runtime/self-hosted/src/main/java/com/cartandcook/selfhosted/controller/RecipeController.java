package com.cartandcook.selfhosted.controller;

import com.cartandcook.core.domain.Recipe;
import com.cartandcook.core.domain.RecipeIngredient;
import com.cartandcook.selfhosted.contracts.RecipeRequest;
import com.cartandcook.selfhosted.contracts.RecipeResponse;
import com.cartandcook.selfhosted.service.RecipeServiceSpring;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeServiceSpring recipeService;

    public RecipeController(RecipeServiceSpring recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes() {
        System.out.println("Getting all recipes");
        List<RecipeResponse> response = recipeService.getAllRecipes()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable("id") Long id) {
        Recipe recipe = recipeService.getRecipeById(id);
        if (recipe == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(recipe));
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> upsertRecipe(@RequestBody RecipeRequest request) {
        List<RecipeIngredient> ingredients = request.getIngredients().stream().map(ingredient -> {
            ingredient.setName(ingredient.getName().toLowerCase());
            return ingredient;
        }).toList();
        Recipe recipe = new Recipe(
                request.getId(),
                request.getName().toLowerCase(),
                request.getCategory().toLowerCase(),
                request.getDescription().toLowerCase(),
                ingredients
        );
        Recipe saved = recipeService.upsertRecipe(recipe);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable("id") Long id) {
        recipeService.deleteRecipe(id);
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
