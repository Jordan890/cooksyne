package com.cartandcook.adapters.persistencejpa;

import com.cartandcook.core.domain.RecipeIngredient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Database-generated numeric ID
    @SequenceGenerator(
            name = "recipe_seq",
            sequenceName = "recipe_sequence",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    // Store RecipeIngredient list as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<RecipeIngredient> ingredients;
}
