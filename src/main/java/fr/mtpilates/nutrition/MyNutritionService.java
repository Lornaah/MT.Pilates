package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.nutrition.MyNutritionViews.Day;
import fr.mtpilates.nutrition.MyNutritionViews.LegacyBlock;
import fr.mtpilates.nutrition.MyNutritionViews.LegacyItem;
import fr.mtpilates.nutrition.MyNutritionViews.Plan;
import fr.mtpilates.nutrition.MyNutritionViews.PlannedMeal;
import fr.mtpilates.nutrition.MyNutritionViews.ShoppingGroup;
import fr.mtpilates.nutrition.MyNutritionViews.ShoppingLine;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plan actif du membre connecté, mis en forme comme MyNutrition. */
@Service
public class MyNutritionService {

    private static final List<String> LEGACY_ORDER = List.of("breakfast", "lunch", "snack", "dinner");
    private static final Map<String, String> ICONS = Map.of(
            "breakfast", "coffee", "lunch", "utensils", "snack", "cookie", "dinner", "soup", "dessert", "cookie");
    private static final Map<String, String> LABELS = Map.of(
            "breakfast", "Petit-déjeuner", "lunch", "Déjeuner", "snack", "Collation", "dinner", "Dîner", "dessert", "Dessert");

    private final NutritionPlanRepository plans;

    public MyNutritionService(final NutritionPlanRepository plans) {
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public Optional<Plan> activePlan(final CurrentUser user) {
        return plans.findFirstByMemberIdAndActiveTrueOrderByCreatedAtDesc(user.id()).map(MyNutritionService::view);
    }

    private static Plan view(final NutritionPlan p) {
        final boolean showMacros = !p.isHideMacrosFromMember();
        final Map<String, Object> meals = p.getMeals();
        final boolean generated = !meals.isEmpty() && meals.values().iterator().next() instanceof List<?>;
        final Map<String, Object> macros = p.getMacrosTarget().orElse(Map.of());
        final boolean targets = showMacros && (p.getKcalTarget().filter(k -> k != 0).isPresent() || p.getMacrosTarget().isPresent());
        final List<Day> days = NutritionCatalog.DAYS.entrySet().stream()
                .map(d -> generated ? generatedDay(d.getValue(), meals.get(d.getKey()), showMacros)
                        : legacyDay(d.getValue(), meals.get(d.getKey())))
                .toList();
        return new Plan(p.getTitle(), p.getDescription().filter(d -> !d.isEmpty()), p.getPdfUrl().filter(u -> !u.isEmpty()),
                targets, p.getKcalTarget().map(String::valueOf).orElse("—"), stat(macros.get("protein_g")),
                stat(macros.get("carbs_g")), stat(macros.get("fat_g")), generated, days, shopping(p.getShoppingList()));
    }

    private static Day generatedDay(final String label, final Object value, final boolean showMacros) {
        final List<PlannedMeal> planned = maps(value).stream().map(m -> planned(m, showMacros)).toList();
        return new Day(label, !planned.isEmpty(), planned, List.of());
    }

    private static PlannedMeal planned(final Map<?, ?> m, final boolean showMacros) {
        final String type = String.valueOf(m.get("meal_type"));
        final double servings = m.get("servings") instanceof Number n ? n.doubleValue() : 1;
        final Optional<Long> kcal = rounded(m.get("calories"));
        final Optional<Long> protein = rounded(m.get("protein_g"));
        final boolean macros = showMacros && (kcal.filter(k -> k != 0).isPresent() || protein.filter(v -> v != 0).isPresent());
        return new PlannedMeal(ICONS.getOrDefault(type, "utensils"), LABELS.getOrDefault(type, type),
                Boolean.TRUE.equals(m.get("is_free_slot")), String.valueOf(m.get("recipe_title")),
                String.format(Locale.ROOT, "× %.1f portion%s", servings, servings > 1 ? "s" : ""), macros, kcal, protein,
                rounded(m.get("carbs_g")), rounded(m.get("fat_g")));
    }

    /** Jour au format saisi à la main : il a du contenu dès qu'un repas existe, même vide (comme l'original). */
    private static Day legacyDay(final String label, final Object value) {
        final Map<?, ?> day = value instanceof Map<?, ?> map ? map : Map.of();
        final boolean hasContent = LEGACY_ORDER.stream().anyMatch(k -> truthy(day.get(k)));
        final List<LegacyBlock> blocks = LEGACY_ORDER.stream()
                .map(k -> new LegacyBlock(ICONS.get(k), LABELS.get(k),
                        day.get(k) instanceof String s && !s.isEmpty() ? Optional.of(s) : Optional.empty(),
                        maps(day.get(k)).stream().map(MyNutritionService::item).toList()))
                .toList();
        return new Day(label, hasContent, List.of(), blocks);
    }

    private static LegacyItem item(final Map<?, ?> item) {
        final String quantity = text(item.get("quantity"));
        final String unit = text(item.get("unit"));
        return new LegacyItem(quantity.isEmpty() ? Optional.empty() : Optional.of(quantity + ("pièce(s)".equals(unit) ? "" : unit)),
                text(item.get("name")), Optional.of(text(item.get("fromRecipe"))).filter(r -> !r.isEmpty()));
    }

    private static List<ShoppingGroup> shopping(final List<Map<String, Object>> groups) {
        return groups.stream().map(g -> new ShoppingGroup(text(g.get("aisle")), maps(g.get("items")).stream()
                .map(it -> new ShoppingLine(text(it.get("name")),
                        MealPlanTemplateService.grams(it.get("qty")) + " " + text(it.get("unit"))))
                .toList())).toList();
    }

    private static List<Map<?, ?>> maps(final Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).<Map<?, ?>>map(m -> (Map<?, ?>) m).toList();
    }

    private static boolean truthy(final Object value) {
        return value != null && !"".equals(value) && !Boolean.FALSE.equals(value);
    }

    private static Optional<Long> rounded(final Object value) {
        return value instanceof Number n ? Optional.of((long) MealPlanGenerator.jsRound(n.doubleValue())) : Optional.empty();
    }

    private static String stat(final Object value) {
        return value == null ? "—" : MealPlanTemplateService.grams(value);
    }

    private static String text(final Object value) {
        return value == null ? "" : value.toString();
    }
}
