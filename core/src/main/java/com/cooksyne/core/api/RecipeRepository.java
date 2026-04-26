package com.cooksyne.core.api;

import com.cooksyne.core.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {

    Recipe save(Recipe recipe);
    Optional<Recipe> findById(Long id, Long userId);
    List<Recipe> findAll(Long userId);
    void delete(Long id, Long userId);
}
