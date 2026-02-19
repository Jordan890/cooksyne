package com.cartandcook.core.api;

import com.cartandcook.core.domain.GroceryList;

import java.util.List;

public interface GroceryListService {

    GroceryList upsertGroceryList(GroceryList groceryList);
    List<GroceryList> getAllGroceryLists(Long userId);
    GroceryList getGroceryListById(Long id, Long userId);
    void deleteGroceryList(Long id, Long userId);
}
