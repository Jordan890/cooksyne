package com.cartandcook.core.api;

import com.cartandcook.core.domain.Recipe;

import java.util.List;

public interface RecipeService {

    Recipe upsertRecipe(Recipe recipe);
    List<Recipe> getAllRecipes(Long userId);
    Recipe getRecipeById(Long id, Long userId);
    void deleteRecipe(Long id, Long userId);
}
