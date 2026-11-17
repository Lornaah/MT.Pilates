package fr.mtpilates.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Éditeur de plan par membre (NutritionEditor) : 7 jours × 4 repas d'aliments saisis ou importés de recettes. */
@Service
public class NutritionEditorService {

    private static final List<String> MEALS = List.of("breakfast", "lunch", "snack", "dinner");
    private static final String DEFAULT_TITLE = "Plan nutritionnel";

    private final NutritionPlanRepository plans;
    private final ProfileRepository profiles;
    private final RecipeRepository recipes;
    private final MemberGuard guard;
    private final Clock clock;

    public NutritionEditorService(final NutritionPlanRepository plans, final ProfileRepository profiles,
                                  final RecipeRepository recipes, final MemberGuard guard, final Clock clock) {
        this.plans = plans;
        this.profiles = profiles;
        this.recipes = recipes;
        this.guard = guard;
        this.clock = clock;
    }

    /** Plan envoyé par l'éditeur ; `planId` absent : nouveau plan. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EditedPlan(String planId, String title, String description, Map<String, Object> meals) {
    }

    @Transactional(readOnly = true)
    public String memberName(final CurrentUser user, final UUID memberId) {
        guard.requireManage(user, memberId);
        return profiles.findById(memberId).map(Profile::displayName).orElse("…");
    }

    /** État initial : le plan actif le plus récent, ou un plan vide (emptyMeals de l'original). */
    @Transactional(readOnly = true)
    public Map<String, Object> state(final CurrentUser user, final UUID memberId) {
        guard.requireManage(user, memberId);
        final Optional<NutritionPlan> plan = plans.findFirstByMemberIdAndActiveTrueOrderByCreatedAtDesc(memberId);
        final Map<String, Object> state = new LinkedHashMap<>();
        state.put("planId", plan.map(p -> p.getId().toString()).orElse(null));
        state.put("title", plan.map(NutritionPlan::getTitle).orElse(DEFAULT_TITLE));
        state.put("description", plan.flatMap(NutritionPlan::getDescription).orElse(""));
        state.put("meals", plan.map(NutritionPlan::getMeals).filter(m -> !m.isEmpty()).orElseGet(NutritionEditorService::emptyMeals));
        return state;
    }

    /** Recettes du sélecteur : titre, description et ingrédients recopiés dans le repas. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recipeChoices() {
        return recipes.findAllByOrderByTitleAsc().stream().map(r -> {
            final RecipeContent c = r.content();
            final Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", r.getId());
            o.put("title", c.title());
            o.put("description", c.description());
            o.put("ingredients", c.ingredients().stream().map(Ingredient::toJson).toList());
            return o;
        }).toList();
    }

    @Transactional
    public UUID save(final CurrentUser user, final UUID memberId, final EditedPlan edited) {
        guard.requireManage(user, memberId);
        final NutritionPlan plan = Optional.ofNullable(edited.planId()).filter(id -> !id.isBlank()).map(UUID::fromString)
                .flatMap(plans::findById).filter(p -> p.getMemberId().equals(memberId))
                .orElseGet(() -> NutritionPlan.edited(memberId));
        plan.edit(user.id(), Optional.ofNullable(edited.title()).orElse(""),
                Optional.ofNullable(edited.description()).filter(d -> !d.isEmpty()).orElse(null),
                Optional.ofNullable(edited.meals()).orElse(Map.of()), clock.instant());
        return plans.save(plan).getId();
    }

    private static Map<String, Object> emptyMeals() {
        final Map<String, Object> meals = new LinkedHashMap<>();
        NutritionCatalog.DAYS.keySet().forEach(day -> {
            final Map<String, Object> byMeal = new LinkedHashMap<>();
            MEALS.forEach(m -> byMeal.put(m, List.of()));
            meals.put(day, byMeal);
        });
        return meals;
    }
}
