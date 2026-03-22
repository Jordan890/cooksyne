package com.cartandcook.selfhosted.contracts;

import com.cartandcook.core.domain.IngredientQuantity;
import lombok.Data;

import java.util.List;

@Data
public class RecipeResponse {
    private Long id;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private List<IngredientQuantity> ingredients;
    private Integer estimatedCalories;
}
