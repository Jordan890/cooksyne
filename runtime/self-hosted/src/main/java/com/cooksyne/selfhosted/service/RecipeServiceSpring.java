package com.cooksyne.selfhosted.service;

import com.cooksyne.core.api.RecipeRepository;
import com.cooksyne.core.domain.Recipe;
import com.cooksyne.core.services.RecipeServiceImpl;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipeServiceSpring {

    private final RecipeServiceImpl coreService;

    public RecipeServiceSpring(RecipeRepository recipeRepository) {
        // inject core service using core repository
        this.coreService = new RecipeServiceImpl(recipeRepository);
    }

    public Recipe upsertRecipe(Recipe recipe) {
        return coreService.upsertRecipe(recipe);
    }

    public Recipe getRecipeById(Long id, Long userId) {
        return coreService.getRecipeById(id, userId);
    }

    public List<Recipe> getAllRecipes(Long userId) {
        return coreService.getAllRecipes(userId);
    }

    @Transactional
    public void deleteRecipe(Long id, Long userId) {
        coreService.deleteRecipe(id, userId);
    }
}
