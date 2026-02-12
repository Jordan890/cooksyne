package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RecipeIngredient{

    Long id;
    String ingredientName;
    Quantity quantity;
}