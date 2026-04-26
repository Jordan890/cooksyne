package com.cooksyne.selfhosted.controller;

import com.cooksyne.core.domain.GroceryList;
import com.cooksyne.core.domain.User;
import com.cooksyne.selfhosted.contracts.GroceryListRequest;
import com.cooksyne.selfhosted.contracts.GroceryListResponse;
import com.cooksyne.selfhosted.security.CurrentUserProvider;
import com.cooksyne.selfhosted.service.GroceryListServiceSpring;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grocery_list")
public class GroceryListController {

    private final GroceryListServiceSpring groceryListService;
    private final CurrentUserProvider currentUserProvider;

    public GroceryListController(GroceryListServiceSpring groceryListService,
                                 CurrentUserProvider currentUserProvider) {
        this.groceryListService = groceryListService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<GroceryListResponse>> getAllGroceryLists(@AuthenticationPrincipal Jwt jwt) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        List<GroceryListResponse> response = groceryListService.getAllGroceryLists(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroceryListResponse> getGroceryListById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        GroceryList groceryList = groceryListService.getGroceryListById(id, currentUser.getId());
        if (groceryList == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(groceryList));
    }

    @PostMapping
    public ResponseEntity<GroceryListResponse> upsertRecipe(@AuthenticationPrincipal Jwt jwt, @RequestBody GroceryListRequest request) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        GroceryList groceryList = GroceryList.hydrate(
                request.getId(),
                request.getName(),
                request.getDescription(),
                request.getIngredients(),
                currentUser.getId()
        );
        GroceryList saved = groceryListService.upsertGroceryList(groceryList);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public void deleteRecipe(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        User currentUser = currentUserProvider.getCurrentUser(jwt);
        groceryListService.deleteGroceryList(id, currentUser.getId());
    }

    // Mapper to Response DTO
    private GroceryListResponse toResponse(GroceryList groceryList) {
        GroceryListResponse response = new GroceryListResponse();
        response.setId(groceryList.getId() != null ? groceryList.getId() : null);
        response.setName(groceryList.getName());
        response.setDescription(groceryList.getDescription());
        response.setIngredients(groceryList.getIngredients());
        return response;
    }

}
