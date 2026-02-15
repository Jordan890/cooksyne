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
        RecipeEntity recipeEntity = new RecipeEntity();
        if(recipe.getId() != null) {
            recipeEntity.setId(recipe.getId());
        }
        recipeEntity.setName(recipe.getName());
        recipeEntity.setCategory(recipe.getCategory());
        recipeEntity.setDescription(recipe.getDescription());
        recipeEntity.setIngredients(recipe.getIngredients());
        RecipeEntity saved = jpaRepository.save(recipeEntity);
        return toDomain(saved);
    }

    @Override
    public Optional<Recipe> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Recipe> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    private Recipe toDomain(RecipeEntity entity) {
        return Recipe.hydrate(entity.getId(), entity.getName(), entity.getCategory(), entity.getDescription(), entity.getIngredients());
    }
}
