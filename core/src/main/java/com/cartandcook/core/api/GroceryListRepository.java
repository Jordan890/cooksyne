package com.cartandcook.core.api;

import com.cartandcook.core.domain.GroceryList;

import java.util.List;
import java.util.Optional;

public interface GroceryListRepository {
    GroceryList save(GroceryList groceryList);
    Optional<GroceryList> findById(Long id, Long userId);
    List<GroceryList> findAll(Long userId);
    void delete(Long id, Long userId);
}
