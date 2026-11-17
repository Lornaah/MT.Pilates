package fr.mtpilates.nutrition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Listes de valeurs de l'original (types/nutrition.ts, types/food.ts, lib/recipeConfig.ts), dans leur ordre d'affichage. */
public final class NutritionCatalog {

    public static final Map<String, String> FOOD_SOURCES = ordered(
            "manual", "Manuel", "open_food_facts", "OpenFoodFacts", "ciqual", "CIQUAL", "custom_coach", "Coach");

    public static final Map<String, String> FOOD_AISLES = ordered(
            "fruits_legumes", "Fruits & légumes", "viandes_poissons", "Viandes & poissons", "laitages", "Laitages",
            "epicerie_salee", "Épicerie salée", "epicerie_sucree", "Épicerie sucrée", "boulangerie", "Boulangerie",
            "surgeles", "Surgelés", "boissons", "Boissons", "autre", "Autre");

    public static final Map<String, String> RECIPE_CATEGORIES = ordered(
            "breakfast", "Petit-déjeuner", "lunch_dinner", "Déjeuner / Dîner", "snack", "Collation", "dessert", "Dessert");

    public static final Map<String, String> MEAL_TYPES = ordered(
            "breakfast", "Petit-déjeuner", "lunch", "Déjeuner", "dinner", "Dîner", "snack", "Collation");

    public static final Map<String, String> DIETARY_TAGS = ordered(
            "vegetarian", "Végétarien", "vegan", "Vegan", "gluten_free", "Sans gluten", "dairy_free", "Sans lactose",
            "keto", "Kéto", "low_carb", "Faible en glucides");

    public static final Map<String, String> ALLERGENS = ordered(
            "gluten", "Gluten", "dairy", "Lactose", "nuts", "Fruits à coque", "soy", "Soja", "eggs", "Œufs",
            "fish", "Poisson", "shellfish", "Crustacés");

    public static final Map<String, String> EQUIPMENT = ordered(
            "oven", "Four", "stovetop", "Plaque", "microwave", "Micro-ondes", "blender", "Blender", "airfryer", "Airfryer");

    public static final Map<String, String> REGIMES = ordered(
            "omnivore", "Omnivore", "vegetarian", "Végétarien", "vegan", "Vegan", "pescatarian", "Pescétarien");

    public static final Map<String, String> DAYS = ordered(
            "monday", "Lundi", "tuesday", "Mardi", "wednesday", "Mercredi", "thursday", "Jeudi", "friday", "Vendredi",
            "saturday", "Samedi", "sunday", "Dimanche");

    public static final List<String> UNITS = List.of(
            "g", "kg", "ml", "cl", "l", "c. à café", "c. à soupe", "tasse", "pièce(s)", "pincée");

    private NutritionCatalog() {
    }

    public static String aisleLabel(final String value) {
        return value == null ? "Autre" : FOOD_AISLES.getOrDefault(value, "Autre");
    }

    public static String sourceLabel(final String value) {
        return FOOD_SOURCES.getOrDefault(value, value);
    }

    public static String recipeCategoryLabel(final String value) {
        return value == null || value.isEmpty() ? "Autre" : RECIPE_CATEGORIES.getOrDefault(value, value);
    }

    private static Map<String, String> ordered(final String... pairs) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }
}
