package fr.mtpilates.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Aliment du référentiel (table foods). */
@Entity
@Table(name = "foods")
public class Food {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String brand;

    private String barcode;

    @Column(name = "serving_size_g")
    private BigDecimal servingSizeG;

    @Column(name = "calories_per_100g")
    private BigDecimal calories;

    @Column(name = "protein_g_per_100g")
    private BigDecimal protein;

    @Column(name = "carbs_g_per_100g")
    private BigDecimal carbs;

    @Column(name = "fat_g_per_100g")
    private BigDecimal fat;

    @Column(name = "fiber_g_per_100g")
    private BigDecimal fiber;

    @Column(nullable = false)
    private String source = "manual";

    private String aisle;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_by")
    private UUID createdBy;

    protected Food() {
    }

    public Food(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    public void update(final FoodContent c) {
        this.name = c.name();
        this.brand = c.brand();
        this.barcode = c.barcode();
        this.servingSizeG = c.servingSizeG();
        this.calories = c.calories();
        this.protein = c.protein();
        this.carbs = c.carbs();
        this.fat = c.fat();
        this.fiber = c.fiber();
        this.source = c.source();
        this.aisle = c.aisle();
        this.isPublic = c.isPublic();
    }

    public UUID getId() {
        return id;
    }

    public FoodContent content() {
        return new FoodContent(name, brand, barcode, servingSizeG, calories, protein, carbs, fat, fiber, source, aisle,
                isPublic);
    }
}
