package com.cartandcook.core.domain;

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

    public static Recipe hydrate(Long id, String name, String category, String description, List<RecipeIngredient> ingredients) {
        List<RecipeIngredient> lowerIngredients = ingredients.stream().peek(ingredient ->
                ingredient.setName(ingredient.getName().toLowerCase())).toList();
        return new Recipe(id, name.toLowerCase(), category.toLowerCase(), description.toLowerCase(), lowerIngredients);
    }
}