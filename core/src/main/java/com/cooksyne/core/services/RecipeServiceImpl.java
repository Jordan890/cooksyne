package com.cooksyne.core.services;

import com.cooksyne.core.api.RecipeRepository;
import com.cooksyne.core.api.RecipeService;
import com.cooksyne.core.domain.Recipe;

import java.util.List;

public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public Recipe upsertRecipe(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    @Override
    public List<Recipe> getAllRecipes(Long userId) {
        return recipeRepository.findAll(userId);
    }

    @Override
    public Recipe getRecipeById(Long id, Long userId) {
        return recipeRepository.findById(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with id: " + id));
    }

    @Override
    public void deleteRecipe(Long id, Long userId) {
        recipeRepository.delete(id, userId);
    }
}

