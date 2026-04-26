package com.cooksyne.selfhosted.contracts;

import com.cooksyne.core.domain.IngredientQuantity;
import lombok.Data;

import java.util.List;

@Data
public class GroceryListResponse {
    private Long id;
    private String name;
    private String description;
    private List<IngredientQuantity> ingredients;
}
