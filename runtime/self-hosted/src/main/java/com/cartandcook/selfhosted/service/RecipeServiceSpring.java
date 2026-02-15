package com.cartandcook.selfhosted.service;

import com.cartandcook.core.api.RecipeRepository;
import com.cartandcook.core.domain.Recipe;
import com.cartandcook.core.services.RecipeServiceImpl;
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

    public Recipe getRecipeById(Long id) {
        return coreService.getRecipeById(id);
    }

    public List<Recipe> getAllRecipes() {
        return coreService.getAllRecipes();
    }

    public void deleteRecipe(Long id) {
        coreService.deleteRecipe(id);
    }
}
