package com.cooksyne.core.api;

import com.cooksyne.core.domain.Recipe;

import java.util.List;

public interface RecipeService {

    Recipe upsertRecipe(Recipe recipe);
    List<Recipe> getAllRecipes(Long userId);
    Recipe getRecipeById(Long id, Long userId);
    void deleteRecipe(Long id, Long userId);
}
