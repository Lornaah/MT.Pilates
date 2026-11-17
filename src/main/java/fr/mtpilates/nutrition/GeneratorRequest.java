package fr.mtpilates.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/** Configuration du générateur envoyée par l'assistant (MealPlanConfig de l'original). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratorRequest(double kcalTarget, double proteinG, double carbsG, double fatG, int mealsPerDay,
                               int freeMealSlots, boolean includeDesserts, List<String> dietaryTags,
                               List<String> excludedAllergens, List<String> availableEquipment,
                               Map<String, List<Map<String, Object>>> meals) {

    MealPlanGenerator.Config toConfig() {
        return new MealPlanGenerator.Config(kcalTarget, proteinG, carbsG, fatG, mealsPerDay, freeMealSlots, includeDesserts,
                orEmpty(dietaryTags), orEmpty(excludedAllergens), orEmpty(availableEquipment));
    }

    private static List<String> orEmpty(final List<String> values) {
        return values == null ? List.of() : values;
    }
}
