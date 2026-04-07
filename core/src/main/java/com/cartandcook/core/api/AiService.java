package com.cartandcook.core.api;

import com.cartandcook.core.domain.RecipeAnalysis;

public interface AiService {

    RecipeAnalysis analyzeFoodByTitle(String dishTitle);

    RecipeAnalysis analyzeRecipeByText(String extractedText);

    Integer estimateCalories(String recipeName, String ingredientsSummary, String servingSize);
}
