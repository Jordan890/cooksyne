package com.cooksyne.core.api;

import com.cooksyne.core.domain.RecipeAnalysis;

/**
 * Fallback AiService used when no AI provider is configured.
 */
public class DisabledAiService implements AiService {

    private static final String MSG = "AI functionality is not available. Set 'cooksyne.ai.provider' to enable AI features.";

    @Override
    public RecipeAnalysis analyzeFoodByTitle(String dishTitle) {
        throw new AiServiceException(MSG);
    }

    @Override
    public RecipeAnalysis analyzeRecipeByText(String extractedText) {
        throw new AiServiceException(MSG);
    }

    @Override
    public Integer estimateCalories(String recipeName, String ingredientsSummary, String servingSize) {
        throw new AiServiceException(MSG);
    }
}
