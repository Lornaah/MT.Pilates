package fr.mtpilates.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Recette du carnet (table recipes). */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String category = "lunch";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> ingredients = new ArrayList<>();

    private String instructions;

    private Integer calories;

    @Column(name = "protein_g")
    private BigDecimal protein;

    @Column(name = "carbs_g")
    private BigDecimal carbs;

    @Column(name = "fat_g")
    private BigDecimal fat;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "prep_time_minutes")
    private Integer prepMinutes;

    @Column(name = "cook_time_minutes")
    private Integer cookMinutes;

    @Column(nullable = false)
    private int servings = 1;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_tags", columnDefinition = "text[]", nullable = false)
    private List<String> dietaryTags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> allergens = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "equipment_needed", columnDefinition = "text[]", nullable = false)
    private List<String> equipment = new ArrayList<>();

    @Column(name = "is_dessert", nullable = false)
    private boolean dessert;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "meal_types", columnDefinition = "text[]", nullable = false)
    private List<String> mealTypes = new ArrayList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    protected Recipe() {
    }

    public Recipe(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    public void update(final RecipeContent c) {
        this.title = c.title();
        this.description = c.description();
        this.category = c.category();
        this.calories = c.calories();
        this.protein = c.protein();
        this.carbs = c.carbs();
        this.fat = c.fat();
        this.instructions = c.instructions();
        this.ingredients = new ArrayList<>(c.ingredients().stream().map(Ingredient::toJson).toList());
        this.imageUrl = c.imageUrl();
        this.prepMinutes = c.prepMinutes();
        this.cookMinutes = c.cookMinutes();
        this.servings = c.servings();
        this.dietaryTags = new ArrayList<>(c.dietaryTags());
        this.allergens = new ArrayList<>(c.allergens());
        this.equipment = new ArrayList<>(c.equipment());
        this.mealTypes = new ArrayList<>(c.mealTypes());
        this.dessert = c.dessert();
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public RecipeContent content() {
        return new RecipeContent(title, description, category, calories, protein, carbs, fat, instructions,
                ingredients == null ? List.of() : ingredients.stream().map(Ingredient::of).toList(), imageUrl, prepMinutes,
                cookMinutes, servings, copy(dietaryTags), copy(allergens), copy(equipment), copy(mealTypes), dessert);
    }

    private static List<String> copy(final List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
