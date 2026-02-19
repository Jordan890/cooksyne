package com.cartandcook.adapters.persistencejpa;

import com.cartandcook.core.api.GroceryListRepository;
import com.cartandcook.core.domain.GroceryList;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class GroceryListRepositoryJpa implements GroceryListRepository {

    private final SpringDataGroceryListRepository jpaRepository;

    public GroceryListRepositoryJpa(SpringDataGroceryListRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public GroceryList save(GroceryList groceryList) {

        System.out.println("Grocery list owner is: " + groceryList.getOwnerId());

        GroceryListEntity groceryListEntity;

        if(groceryList.getId() != null) {
            groceryListEntity = jpaRepository.findByIdAndOwnerId(groceryList.getId(), groceryList.getOwnerId()).orElse(null);
            if(groceryListEntity == null) {
                throw new IllegalArgumentException("Grocery list with id " + groceryList.getId() + " not found for user " + groceryList.getOwnerId());
            }
        } else {
            groceryListEntity = new GroceryListEntity();
            groceryListEntity.setOwnerId(groceryList.getOwnerId());
        }
        groceryListEntity.setName(groceryList.getName());
        groceryListEntity.setDescription(groceryList.getDescription());
        groceryListEntity.setIngredients(groceryList.getIngredients());
        GroceryListEntity saved = jpaRepository.save(groceryListEntity);
        return toDomain(saved);
    }

    @Override
    public Optional<GroceryList> findById(Long id, Long userId) {
        return jpaRepository.findByIdAndOwnerId(id, userId).map(this::toDomain);
    }

    @Override
    public List<GroceryList> findAll(Long userId) {
        return jpaRepository.findByOwnerId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, Long userId) {
        jpaRepository.deleteByIdAndOwnerId(id, userId);
    }

    private GroceryList toDomain(GroceryListEntity entity) {
        return GroceryList.hydrate(entity.getId(), entity.getName(), entity.getDescription(), entity.getIngredients(), entity.getOwnerId());
    }
}
