package com.cooksyne.selfhosted.contracts;

import com.cooksyne.core.domain.IngredientQuantity;
import com.cooksyne.core.domain.Quantity;
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
    private Quantity servingSize;
}
