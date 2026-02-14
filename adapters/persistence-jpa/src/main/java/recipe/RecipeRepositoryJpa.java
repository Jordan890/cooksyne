package recipe;

import api.RecipeRepository;
import domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RecipeRepositoryJpa implements RecipeRepository {

    private final JpaRepository<RecipeEntity, Long> jpaRepository;

    public RecipeRepositoryJpa(JpaRepository<RecipeEntity, Long> jpaRepository) {
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
        return Recipe.rehydrate(entity.getId(), entity.getName(), entity.getCategory(), entity.getDescription(), entity.getIngredients());
    }
}
