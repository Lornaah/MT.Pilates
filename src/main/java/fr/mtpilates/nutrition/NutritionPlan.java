package fr.mtpilates.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Plan nutritionnel d'un membre (table nutrition_plans). `meals` a deux formats selon l'origine
 * (jour → liste de repas générés, ou jour → repas par type) : il reste un objet JSON brut.
 */
@Entity
@Table(name = "nutrition_plans")
public class NutritionPlan {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "coach_id")
    private UUID coachId;

    @Column(nullable = false)
    private String title;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> meals = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shopping_list", nullable = false)
    private List<Map<String, Object>> shoppingList = new ArrayList<>();

    @Column(name = "kcal_target")
    private Integer kcalTarget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "macros_target")
    private Map<String, Object> macrosTarget;

    @Column(name = "generated_from_template", nullable = false)
    private boolean generatedFromTemplate;

    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NutritionPlan() {
    }

    /** Matérialisation d'une semaine de menu type pour un membre (assignToMember de l'original). */
    public static NutritionPlan fromTemplate(final MealPlanTemplate template, final Map<String, Object> days,
                                             final UUID memberId, final UUID coachId, final Instant now) {
        final NutritionPlan plan = new NutritionPlan();
        plan.id = UUID.randomUUID();
        plan.memberId = memberId;
        plan.coachId = coachId;
        plan.title = template.getTitle();
        plan.description = template.getDescription().orElse(null);
        plan.kcalTarget = template.getKcalTarget().orElse(null);
        plan.macrosTarget = template.getMacrosTarget().orElse(null);
        plan.meals = new LinkedHashMap<>(days);
        plan.generatedFromTemplate = true;
        plan.sourceTemplateId = template.getId();
        plan.updatedAt = now;
        return plan;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isActive() {
        return active;
    }

    public Optional<Integer> getKcalTarget() {
        return Optional.ofNullable(kcalTarget);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
