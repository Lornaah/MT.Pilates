package fr.mtpilates.nutrition;

import static fr.mtpilates.nutrition.NutritionAccess.requireStaff;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Générateur de plan repas : élèves proposés, génération, liste de courses et envoi. */
@Service
public class MealPlanGeneratorService {

    private final RecipeRepository recipes;
    private final ProfileRepository profiles;
    private final NutritionPlanRepository plans;
    private final MealPlanTemplateService templates;
    private final MemberGuard guard;
    private final Clock clock;

    public MealPlanGeneratorService(final RecipeRepository recipes, final ProfileRepository profiles,
                                    final NutritionPlanRepository plans, final MealPlanTemplateService templates,
                                    final MemberGuard guard, final Clock clock) {
        this.recipes = recipes;
        this.profiles = profiles;
        this.plans = plans;
        this.templates = templates;
        this.guard = guard;
        this.clock = clock;
    }

    /** Élève proposé : libellé « Prénom Nom » et ajustements de configuration tirés de ses préférences. */
    public record MemberChoice(UUID id, String label, String planTitle, Map<String, Object> configPatch) {
    }

    @Transactional(readOnly = true)
    public List<MemberChoice> members(final CurrentUser user) {
        requireStaff(user);
        final List<UUID> ids = templates.assignableMembers(user).stream().map(NutritionViews.MemberOption::id).toList();
        final Map<UUID, Profile> byId = profiles.findAllById(ids).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(MealPlanGeneratorService::choice).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasRecipes() {
        return recipes.count() > 0;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> generate(final CurrentUser user, final GeneratorRequest request) {
        requireStaff(user);
        return MealPlanGenerator.generateWeek(candidates(), request.toConfig());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> shoppingList(final CurrentUser user,
                                                  final Map<String, List<Map<String, Object>>> meals) {
        requireStaff(user);
        return MealPlanGenerator.shoppingList(candidates().stream()
                .collect(Collectors.toMap(MealPlanGenerator.Candidate::id, Function.identity())), meals);
    }

    /** Envoi : nouveau plan actif du membre (le coach doit le gérer, policy de nutrition_plans). */
    @Transactional
    public UUID send(final CurrentUser user, final GeneratedPlan plan) {
        requireStaff(user);
        if (plan.memberId() == null) {
            throw new IllegalArgumentException("Sélectionnez un élève");
        }
        if (plan.meals() == null || plan.meals().isEmpty()) {
            throw new IllegalArgumentException("Générez le plan avant de l'envoyer");
        }
        guard.requireManage(user, plan.memberId());
        return plans.save(NutritionPlan.generated(plan, user.id(), clock.instant())).getId();
    }

    private List<MealPlanGenerator.Candidate> candidates() {
        return recipes.findAllByOrderByTitleAsc().stream().map(r -> {
            final RecipeContent c = r.content();
            return new MealPlanGenerator.Candidate(r.getId().toString(), c.title(), Optional.ofNullable(c.calories()),
                    value(c.protein()), value(c.carbs()), value(c.fat()), c.mealTypes(), c.dietaryTags(), c.allergens(),
                    c.equipment(), c.ingredients());
        }).toList();
    }

    private static double value(final BigDecimal number) {
        return number == null ? 0 : number.doubleValue();
    }

    private static MemberChoice choice(final Profile p) {
        final String first = p.getFirstName().orElse("");
        final String last = p.getLastName().orElse("");
        return new MemberChoice(p.getId(), first + " " + last, ("Plan repas — " + first + " " + last).trim(),
                configPatch(p.getDietaryPreferences()));
    }

    /** configFromPrefs de l'original : seules les préférences renseignées remplacent la configuration courante. */
    static Map<String, Object> configPatch(final Map<String, Object> prefs) {
        final Map<String, Object> patch = new LinkedHashMap<>();
        number(prefs.get("daily_kcal_target")).ifPresent(v -> patch.put("kcalTarget", v));
        if (prefs.get("macros_split") instanceof Map<?, ?> split) {
            number(split.get("protein_g")).ifPresent(v -> patch.put("proteinG", v));
            number(split.get("carbs_g")).ifPresent(v -> patch.put("carbsG", v));
            number(split.get("fat_g")).ifPresent(v -> patch.put("fatG", v));
        }
        number(prefs.get("meals_per_day")).ifPresent(v -> patch.put("mealsPerDay", v));
        number(prefs.get("free_meal_slots")).ifPresent(v -> patch.put("freeMealSlots", v));
        if (prefs.get("include_desserts") instanceof Boolean b) {
            patch.put("includeDesserts", b);
        }
        nonEmptyList(prefs.get("allergies")).ifPresent(v -> patch.put("excludedAllergens", v));
        nonEmptyList(prefs.get("kitchen_appliances")).ifPresent(v -> patch.put("availableEquipment", v));
        return patch;
    }

    private static Optional<Number> number(final Object value) {
        return value instanceof Number n ? Optional.of(n) : Optional.empty();
    }

    private static Optional<List<?>> nonEmptyList(final Object value) {
        return value instanceof List<?> l && !l.isEmpty() ? Optional.of(l) : Optional.empty();
    }
}
