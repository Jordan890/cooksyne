package com.cooksyne.core.api;

import com.cooksyne.core.domain.GroceryList;

import java.util.List;
import java.util.Optional;

public interface GroceryListRepository {
    GroceryList save(GroceryList groceryList);
    Optional<GroceryList> findById(Long id, Long userId);
    List<GroceryList> findAll(Long userId);
    void delete(Long id, Long userId);
}
