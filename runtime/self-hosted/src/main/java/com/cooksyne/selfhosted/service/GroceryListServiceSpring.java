package com.cooksyne.selfhosted.service;

import com.cooksyne.core.api.GroceryListRepository;
import com.cooksyne.core.domain.GroceryList;
import com.cooksyne.core.services.GroceryListServiceImpl;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroceryListServiceSpring {

    private final GroceryListServiceImpl coreService;

    public GroceryListServiceSpring(GroceryListRepository groceryListRepository) {
        // inject core service using core repository
        this.coreService = new GroceryListServiceImpl(groceryListRepository);
    }

    public GroceryList upsertGroceryList(GroceryList groceryList) {
        return coreService.upsertGroceryList(groceryList);
    }

    public GroceryList getGroceryListById(Long id, Long userId) {
        return coreService.getGroceryListById(id, userId);
    }

    public List<GroceryList> getAllGroceryLists(Long userId) {
        return coreService.getAllGroceryLists(userId);
    }

    @Transactional
    public void deleteGroceryList(Long id, Long userId) {
        coreService.deleteGroceryList(id, userId);
    }
}
