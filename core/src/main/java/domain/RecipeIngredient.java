package domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class RecipeIngredient{

    private Long id;
    private String ingredientName;
    private Quantity quantity;
    private String description;
}