package domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;


@RequiredArgsConstructor
@Getter
public class Recipe {

    private final Long id;
    private final String name;
    private final String category;
    private final String description;
    private final List<RecipeIngredient> ingredients;

    public static Recipe rehydrate(Long id, String name, String category, String description, List<RecipeIngredient> ingredients) {
        return new Recipe(id, name, category, description, ingredients);
    }
}