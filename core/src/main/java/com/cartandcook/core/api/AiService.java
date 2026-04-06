package com.cartandcook.core.api;

import com.cartandcook.core.domain.RecipeAnalysis;

public interface AiService {

    RecipeAnalysis analyzeFoodByTitle(String dishTitle);

    RecipeAnalysis analyzeRecipeByText(String extractedText);
}
