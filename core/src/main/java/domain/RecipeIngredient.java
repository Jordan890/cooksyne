package domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RecipeIngredient{

    private final Long id;
    private final String ingredientName;
    private final Quantity quantity;
    private final String description;
}