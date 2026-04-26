package com.cooksyne.adapters.persistencejpa;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByOwnerId(Long ownerId);

    Optional<RecipeEntity> findByIdAndOwnerId(Long id, Long ownerId);

    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
