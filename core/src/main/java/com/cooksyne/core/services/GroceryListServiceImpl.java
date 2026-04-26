package com.cooksyne.core.services;

import com.cooksyne.core.api.GroceryListRepository;
import com.cooksyne.core.api.GroceryListService;
import com.cooksyne.core.domain.GroceryList;

import java.util.List;

public class GroceryListServiceImpl implements GroceryListService {

    private final GroceryListRepository groceryListRepository;

    public GroceryListServiceImpl(GroceryListRepository groceryListRepository) {
        this.groceryListRepository = groceryListRepository;
    }

    @Override
    public GroceryList upsertGroceryList(GroceryList groceryList) {
        return groceryListRepository.save(groceryList);
    }

    @Override
    public List<GroceryList> getAllGroceryLists(Long userId) {
        return groceryListRepository.findAll(userId);
    }

    @Override
    public GroceryList getGroceryListById(Long id, Long userId) {
        return groceryListRepository.findById(id, userId).orElseThrow(() -> new IllegalArgumentException("Recipe not found with id: " + id));
    }

    @Override
    public void deleteGroceryList(Long id, Long userId) {
        groceryListRepository.delete(id, userId);
    }
}
