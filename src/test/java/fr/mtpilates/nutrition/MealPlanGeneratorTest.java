package fr.mtpilates.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.nutrition.MealPlanGenerator.Candidate;
import fr.mtpilates.nutrition.MealPlanGenerator.Config;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Le port Java doit produire exactement ce que produisait lib/mealPlanGenerator.ts : la référence a été générée en
 * exécutant le fichier TypeScript de l'original sur les mêmes recettes et configurations.
 */
class MealPlanGeneratorTest {

    private static final List<Ingredient> INGREDIENTS = List.of(new Ingredient("Riz", "100", "g"),
            new Ingredient("riz", "1.5 kg", "g"), new Ingredient("Sel", "pincée", "pincée"));

    private static final List<Candidate> RECIPES = List.of(
            recipe("a", "Avoine", 380, 12, 62, 8.5, List.of("breakfast"), List.of("vegan"), List.of(), List.of()),
            recipe("b", "Bowl", 520, 42, 48, 14, List.of("lunch", "dinner"), List.of(), List.of(), List.of("oven")),
            recipe("c", "Curry", 610, 30, 70, 22, List.of("lunch", "dinner"), List.of(), List.of("nuts"), List.of()),
            recipe("d", "Dal", 450, 24, 60, 9, List.of(), List.of(), List.of(), List.of()),
            recipe("e", "Energy", null, 5, 20, 8, List.of("snack"), List.of(), List.of(), List.of()),
            recipe("f", "Fromage blanc", 150, 18, 8, 3, List.of("snack", "breakfast"), List.of(), List.of(), List.of()));

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void matchesTheOriginalOnReferenceCases() throws IOException {
        final JsonNode reference = reference();
        final Map<String, Config> cases = new LinkedHashMap<>();
        cases.put("base", config(2000, 3, 0, false, List.of(), List.of(), List.of()));
        cases.put("five", config(2600, 5, 1, true, List.of(), List.of(), List.of()));
        cases.put("restricted", config(1500, 3, 0, false, List.of(), List.of("nuts"), List.of("stovetop")));
        cases.put("vegan", config(2000, 4, 0, false, List.of("vegan"), List.of(), List.of()));
        final Map<String, Candidate> byId = RECIPES.stream().collect(Collectors.toMap(Candidate::id, Function.identity()));

        for (final Map.Entry<String, Config> c : cases.entrySet()) {
            final Map<String, List<Map<String, Object>>> meals = MealPlanGenerator.generateWeek(RECIPES, c.getValue());
            assertThat(tree(meals)).as(c.getKey()).isEqualTo(reference.get(c.getKey()).get("meals"));
            assertThat(tree(MealPlanGenerator.shoppingList(byId, meals))).as(c.getKey() + " courses")
                    .isEqualTo(reference.get(c.getKey()).get("shopping"));
        }
    }

    @Test
    void parsesQuantitiesLikeParseFloat() {
        assertThat(MealPlanGenerator.parseFloat(" 1.5 kg")).contains(1.5);
        assertThat(MealPlanGenerator.parseFloat("1/2")).contains(1.0);
        assertThat(MealPlanGenerator.parseFloat(".5")).contains(0.5);
        assertThat(MealPlanGenerator.parseFloat("pincée")).isEmpty();
    }

    @Test
    void servingsAreClampedAndRoundedToHalfPortions() {
        assertThat(MealPlanGenerator.optimalServings(Optional.of(100), 1000)).isEqualTo(2);
        assertThat(MealPlanGenerator.optimalServings(Optional.of(1000), 100)).isEqualTo(0.5);
        assertThat(MealPlanGenerator.optimalServings(Optional.of(400), 500)).isEqualTo(1.5);
        assertThat(MealPlanGenerator.optimalServings(Optional.empty(), 500)).isEqualTo(1);
    }

    private JsonNode tree(final Object value) throws IOException {
        return json.readTree(json.writeValueAsString(value));
    }

    private JsonNode reference() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/nutrition/meal-plan-generator-reference.json")) {
            return json.readTree(in);
        }
    }

    private static Config config(final double kcal, final int meals, final int free, final boolean desserts,
                                 final List<String> tags, final List<String> allergens, final List<String> equipment) {
        return new Config(kcal, 140, 200, 70, meals, free, desserts, tags, allergens, equipment);
    }

    private static Candidate recipe(final String id, final String title, final Integer kcal, final double p, final double c,
                                    final double f, final List<String> mealTypes, final List<String> tags,
                                    final List<String> allergens, final List<String> equipment) {
        return new Candidate(id, title, Optional.ofNullable(kcal), p, c, f, mealTypes, tags, allergens, equipment, INGREDIENTS);
    }
}
