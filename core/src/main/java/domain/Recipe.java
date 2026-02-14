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
}