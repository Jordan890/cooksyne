package com.cooksyne.adapters.persistencejpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataGroceryListRepository extends JpaRepository<GroceryListEntity, Long> {
    List<GroceryListEntity> findByOwnerId(Long ownerId);

    Optional<GroceryListEntity> findByIdAndOwnerId(Long id, Long ownerId);

    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
