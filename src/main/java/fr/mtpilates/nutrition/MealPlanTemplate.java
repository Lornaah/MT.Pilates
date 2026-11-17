package fr.mtpilates.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Menu type réutilisable (table meal_plan_templates) : N semaines de 7 jours de repas. */
@Entity
@Table(name = "meal_plan_templates")
public class MealPlanTemplate {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "cover_color")
    private String coverColor;

    @Column(name = "kcal_target")
    private Integer kcalTarget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "macros_target")
    private Map<String, Object> macrosTarget;

    @Column(name = "meals_per_day", nullable = false)
    private int mealsPerDay = 3;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_preferences")
    private Map<String, Object> dietaryPreferences;

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> weeks = new ArrayList<>();

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealPlanTemplate() {
    }

    public MealPlanTemplate(final UUID createdBy, final Instant now) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
        this.updatedAt = now;
    }

    /** Enregistrement depuis l'éditeur (« Brouillon » ou « Publier »). */
    public void update(final TemplateContent c, final Instant now) {
        this.title = c.title();
        this.description = c.description();
        this.status = c.status();
        this.coverColor = c.coverColor();
        this.kcalTarget = c.kcalTarget();
        this.macrosTarget = c.macrosTarget();
        this.mealsPerDay = c.mealsPerDay();
        this.dietaryPreferences = c.dietaryPreferences();
        this.weeks = new ArrayList<>(c.weeks());
        this.durationWeeks = c.weeks().size();
        this.updatedAt = now;
    }

    /** Le trigger updated_at de l'original s'appliquait aussi à ce compteur : le menu remonte en tête de liste. */
    public void bumpUsage(final Instant now) {
        usageCount++;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getStatus() {
        return status;
    }

    public Optional<String> getCoverColor() {
        return Optional.ofNullable(coverColor);
    }

    public Optional<Integer> getKcalTarget() {
        return Optional.ofNullable(kcalTarget);
    }

    public Optional<Map<String, Object>> getMacrosTarget() {
        return Optional.ofNullable(macrosTarget);
    }

    public int getMealsPerDay() {
        return mealsPerDay;
    }

    public Optional<Map<String, Object>> getDietaryPreferences() {
        return Optional.ofNullable(dietaryPreferences);
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public List<Map<String, Object>> getWeeks() {
        return weeks == null ? List.of() : List.copyOf(weeks);
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
