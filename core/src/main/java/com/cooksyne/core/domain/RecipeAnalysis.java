package com.cooksyne.core.domain;

import java.util.List;

public class RecipeAnalysis {

    private String title;
    private String category;
    private String description;
    private List<IngredientEstimate> ingredients;
    private Integer estimatedCalories;

    public RecipeAnalysis() {
    }

    public RecipeAnalysis(String title, String category, String description, List<IngredientEstimate> ingredients,
            Integer estimatedCalories) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.ingredients = ingredients;
        this.estimatedCalories = estimatedCalories;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<IngredientEstimate> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientEstimate> ingredients) {
        this.ingredients = ingredients;
    }

    public Integer getEstimatedCalories() {
        return estimatedCalories;
    }

    public void setEstimatedCalories(Integer estimatedCalories) {
        this.estimatedCalories = estimatedCalories;
    }
}
