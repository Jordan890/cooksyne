package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@AllArgsConstructor
@Getter
@Setter
public class Recipe {

    Long id;
    String name;
    List<RecipeIngredient> ingredients;
}