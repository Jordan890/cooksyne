package com.cartandcook.adapters.persistencejpa;

import com.cartandcook.core.api.RecipeRepository;
import com.cartandcook.core.domain.Recipe;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RecipeRepositoryJpa implements RecipeRepository {

    private final SpringDataRecipeRepository jpaRepository;

    public RecipeRepositoryJpa(SpringDataRecipeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Recipe save(Recipe recipe) {
        RecipeEntity recipeEntity;
        if (recipe.getId() != null) {
            recipeEntity = jpaRepository.findByIdAndOwnerId(recipe.getId(), recipe.getOwnerId())
                    .orElse(null);
            if (recipeEntity == null) {
                throw new IllegalArgumentException(
                        "Recipe with id " + recipe.getId() + " not found for user " + recipe.getOwnerId());
            }
        } else {
            recipeEntity = new RecipeEntity();
            recipeEntity.setOwnerId(recipe.getOwnerId());
        }
        recipeEntity.setName(recipe.getName());
        recipeEntity.setCategory(recipe.getCategory());
        recipeEntity.setDescription(recipe.getDescription());
        recipeEntity.setImageUrl(recipe.getImageUrl());
        recipeEntity.setIngredients(recipe.getIngredients());
        recipeEntity.setEstimatedCalories(recipe.getEstimatedCalories());
        RecipeEntity saved = jpaRepository.save(recipeEntity);
        return toDomain(saved);
    }

    @Override
    public Optional<Recipe> findById(Long id, Long userId) {
        return jpaRepository.findByIdAndOwnerId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Recipe> findAll(Long userId) {
        return jpaRepository.findByOwnerId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, Long userId) {
        jpaRepository.deleteByIdAndOwnerId(id, userId);
    }

    private Recipe toDomain(RecipeEntity entity) {
        return Recipe.hydrate(entity.getId(), entity.getName(), entity.getCategory(), entity.getDescription(),
                entity.getImageUrl(), entity.getIngredients(), entity.getEstimatedCalories(), entity.getOwnerId());
    }
}
