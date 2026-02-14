package recipe;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Database-generated numeric ID
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
    private String ingredientsJson;
}
