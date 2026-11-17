package fr.mtpilates.nutrition;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port exact de lib/mealPlanGenerator.ts : algorithme glouton déterministe (repas les plus proches de la cible
 * par créneau, sans répéter une recette deux jours de suite) et liste de courses agrégée.
 * Les repas produits gardent le format JSON de l'original (PlannedMeal).
 */
public final class MealPlanGenerator {

    /** Configuration du générateur (GeneratorConfig). */
    public record Config(double kcalTarget, double proteinTarget, double carbsTarget, double fatTarget, int mealsPerDay,
                         int freeMealSlots, boolean includeDesserts, List<String> dietaryTags,
                         List<String> excludedAllergens, List<String> availableEquipment) {
    }

    /** Recette candidate : macros pour une portion, contraintes et ingrédients. */
    public record Candidate(String id, String title, Optional<Integer> calories, double protein, double carbs, double fat,
                            List<String> mealTypes, List<String> dietaryTags, List<String> allergens,
                            List<String> equipment, List<Ingredient> ingredients) {
    }

    // Protéines et lipides pèsent plus que les glucides ; les kcal, déjà sur une grande échelle, restent à 1.
    private static final double WEIGHT_KCAL = 1.0;
    private static final double WEIGHT_PROTEIN = 4.0;
    private static final double WEIGHT_CARBS = 2.0;
    private static final double WEIGHT_FAT = 4.0;

    private static final double MIN_SERVINGS = 0.5;
    private static final double MAX_SERVINGS = 2;
    private static final double SERVINGS_STEP = 0.5;
    private static final double SHOPPING_FACTOR = 100;

    private static final Map<Integer, List<String>> SEQUENCES = Map.of(
            3, List.of("breakfast", "lunch", "dinner"),
            4, List.of("breakfast", "lunch", "snack", "dinner"),
            5, List.of("breakfast", "snack", "lunch", "snack", "dinner"),
            6, List.of("breakfast", "snack", "lunch", "snack", "dinner", "snack"));

    /** Préfixe numérique lu par `parseFloat` (« 1.5 kg » → 1.5, « 1/2 » → 1). */
    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)");

    private MealPlanGenerator() {
    }

    public static Map<String, List<Map<String, Object>>> generateWeek(final List<Candidate> recipes, final Config cfg) {
        final List<String> sequence = SEQUENCES.getOrDefault(cfg.mealsPerDay(), SEQUENCES.get(3));
        final Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        Set<String> usedYesterday = new HashSet<>();
        for (final String day : MealPlanTemplateService.DAYS) {
            final Set<String> usedToday = new HashSet<>();
            final List<Map<String, Object>> meals = new ArrayList<>();
            for (int i = 0; i < sequence.size(); i++) {
                meals.add(slot(recipes, cfg, sequence, i, usedToday, usedYesterday));
            }
            if (cfg.includeDesserts()) {
                meals.add(freeSlot("Dessert (libre)", "dessert"));
            }
            result.put(day, meals);
            usedYesterday = usedToday;
        }
        return result;
    }

    private static Map<String, Object> slot(final List<Candidate> recipes, final Config cfg, final List<String> sequence,
                                            final int index, final Set<String> usedToday, final Set<String> usedYesterday) {
        final String mealType = sequence.get(index);
        if (index >= sequence.size() - cfg.freeMealSlots()) {
            return freeSlot("Repas libre", mealType);
        }
        final List<Candidate> eligible = recipes.stream()
                .filter(r -> eligible(r, cfg, mealType) && !usedToday.contains(r.id()) && !usedYesterday.contains(r.id()))
                .toList();
        final List<Candidate> pool = eligible.isEmpty()
                ? recipes.stream().filter(r -> eligible(r, cfg, mealType) && !usedToday.contains(r.id())).toList()
                : eligible;
        if (pool.isEmpty()) {
            return freeSlot("Aucune recette éligible", mealType);
        }
        final double slotKcal = cfg.kcalTarget() / sequence.size();
        final int remaining = sequence.size() - index;
        Candidate best = null;
        double bestServings = 1;
        double bestScore = Double.MAX_VALUE;
        for (final Candidate r : pool) {
            final double servings = optimalServings(r.calories(), slotKcal);
            final double score = score(r, servings, remaining, cfg);
            if (best == null || score < bestScore) {
                best = r;
                bestServings = servings;
                bestScore = score;
            }
        }
        usedToday.add(best.id());
        return plannedMeal(best, mealType, bestServings);
    }

    static boolean eligible(final Candidate r, final Config cfg, final String mealType) {
        if (!r.mealTypes().isEmpty() && !r.mealTypes().contains(mealType)) {
            return false;
        }
        if (!r.dietaryTags().containsAll(cfg.dietaryTags())) {
            return false;
        }
        if (cfg.excludedAllergens().stream().anyMatch(r.allergens()::contains)) {
            return false;
        }
        return cfg.availableEquipment().isEmpty() || r.equipment().isEmpty()
                || cfg.availableEquipment().containsAll(r.equipment());
    }

    /** Distance L1 pondérée à la cible du créneau : cible du jour divisée par (repas restants + 1). */
    private static double score(final Candidate r, final double servings, final int remaining, final Config cfg) {
        final double divisor = Math.max(1, remaining + 1);
        return Math.abs(r.calories().orElse(0) * servings - cfg.kcalTarget() / divisor) * WEIGHT_KCAL
                + Math.abs(r.protein() * servings - cfg.proteinTarget() / divisor) * WEIGHT_PROTEIN
                + Math.abs(r.carbs() * servings - cfg.carbsTarget() / divisor) * WEIGHT_CARBS
                + Math.abs(r.fat() * servings - cfg.fatTarget() / divisor) * WEIGHT_FAT;
    }

    /** Multiplicateur de portions vers le budget kcal du créneau, borné à [0,5 ; 2] et arrondi à la demi-portion. */
    static double optimalServings(final Optional<Integer> recipeKcal, final double slotKcal) {
        if (recipeKcal.isEmpty() || recipeKcal.get() <= 0) {
            return 1;
        }
        final double clamped = Math.min(MAX_SERVINGS, Math.max(MIN_SERVINGS, slotKcal / recipeKcal.get()));
        final double inverse = 1 / SERVINGS_STEP;
        return jsRound(clamped * inverse) / inverse;
    }

    private static Map<String, Object> plannedMeal(final Candidate r, final String mealType, final double servings) {
        final Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("recipe_id", r.id());
        meal.put("recipe_title", r.title());
        meal.put("meal_type", mealType);
        meal.put("servings", number(servings));
        meal.put("calories", (long) jsRound(r.calories().orElse(0) * servings));
        meal.put("protein_g", (long) jsRound(r.protein() * servings));
        meal.put("carbs_g", (long) jsRound(r.carbs() * servings));
        meal.put("fat_g", (long) jsRound(r.fat() * servings));
        return meal;
    }

    private static Map<String, Object> freeSlot(final String title, final String mealType) {
        final Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("recipe_id", "");
        meal.put("recipe_title", title);
        meal.put("meal_type", mealType);
        meal.put("servings", 1);
        meal.put("is_free_slot", true);
        return meal;
    }

    /**
     * Liste de courses (buildShoppingList) : quantités sommées par (nom en minuscules, unité), arrondies au centième,
     * dans un seul rayon « autre », triées par nom en français.
     */
    public static List<Map<String, Object>> shoppingList(final Map<String, Candidate> recipesById,
                                                         final Map<String, List<Map<String, Object>>> meals) {
        final Map<String, Object[]> items = new LinkedHashMap<>();
        meals.values().forEach(day -> day.forEach(m -> addIngredients(recipesById, m, items)));
        final Collator french = Collator.getInstance(Locale.FRENCH);
        final List<Map<String, Object>> sorted = items.values().stream()
                .sorted((a, b) -> french.compare((String) a[0], (String) b[0]))
                .map(it -> item((String) it[0], jsRound((double) it[1] * SHOPPING_FACTOR) / SHOPPING_FACTOR, (String) it[2]))
                .toList();
        final Map<String, Object> group = new LinkedHashMap<>();
        group.put("aisle", "autre");
        group.put("items", sorted);
        return List.of(group);
    }

    private static void addIngredients(final Map<String, Candidate> recipesById, final Map<String, Object> meal,
                                       final Map<String, Object[]> items) {
        final Object recipeId = meal.get("recipe_id");
        if (Boolean.TRUE.equals(meal.get("is_free_slot")) || recipeId == null || "".equals(recipeId)) {
            return;
        }
        final Candidate recipe = recipesById.get(recipeId.toString());
        if (recipe == null) {
            return;
        }
        final double servings = meal.get("servings") instanceof Number n ? n.doubleValue() : Double.NaN;
        for (final Ingredient ing : recipe.ingredients()) {
            final double qty = parseFloat(ing.quantity()).map(q -> q * servings).filter(Double::isFinite).orElse(0.0);
            final String key = ing.name().toLowerCase(Locale.ROOT) + "|" + ing.unit();
            final Object[] existing = items.get(key);
            if (existing == null) {
                items.put(key, new Object[] {ing.name(), qty, ing.unit()});
            } else {
                existing[1] = (double) existing[1] + qty;
            }
        }
    }

    static Optional<Double> parseFloat(final String value) {
        final Matcher m = LEADING_NUMBER.matcher(value == null ? "" : value);
        return m.find() ? Optional.of(Double.parseDouble(m.group(1))) : Optional.empty();
    }

    private static Map<String, Object> item(final String name, final double qty, final String unit) {
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("qty", number(qty));
        item.put("unit", unit);
        return item;
    }

    /** Math.round de JavaScript : arrondi à l'entier supérieur pour les demis, y compris négatifs. */
    static double jsRound(final double value) {
        return Math.floor(value + 0.5);
    }

    /** Un nombre entier est écrit sans décimale, comme JSON.stringify. */
    private static Number number(final double value) {
        return value == Math.rint(value) && Math.abs(value) < Long.MAX_VALUE ? (Number) (long) value : (Number) value;
    }
}
