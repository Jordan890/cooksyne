package com.cartandcook.adapters.persistencejpa;


import com.cartandcook.core.domain.IngredientQuantity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "grocery_lists")
@Getter
@Setter
@NoArgsConstructor
public class GroceryListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grocery_list_seq")
    @SequenceGenerator(
            name = "grocery_list_seq",
            sequenceName = "grocery_list_sequence",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    // Store RecipeIngredient list as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<IngredientQuantity> ingredients;

    // --- new field for ownership ---
    @Column(nullable = false)
    private Long ownerId;
}
