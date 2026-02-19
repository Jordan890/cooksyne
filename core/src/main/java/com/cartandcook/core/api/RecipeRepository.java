package com.cartandcook.core.api;

import com.cartandcook.core.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {

    Recipe save(Recipe recipe);
    Optional<Recipe> findById(Long id, Long userId);
    List<Recipe> findAll(Long userId);
    void delete(Long id, Long userId);
}
