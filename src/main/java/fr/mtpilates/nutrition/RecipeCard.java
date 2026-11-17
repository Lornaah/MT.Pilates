package fr.mtpilates.nutrition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Carte du carnet de recettes (RecipeCard). Les listes brutes servent aux filtres côté navigateur,
 * `search` regroupe titre, description et noms d'ingrédients en minuscules.
 */
public record RecipeCard(UUID id, String title, Optional<String> description, String category, String categoryLabel,
                         Optional<Integer> calories, int totalMinutes, int servings, int ingredientCount,
                         List<String> dietaryLabels, int extraDietary, Optional<String> protein, Optional<String> carbs,
                         Optional<String> fat, Optional<String> imageUrl, String monogram, boolean mine, String search,
                         List<String> dietaryTags, List<String> allergens, List<String> equipment, List<String> mealTypes) {

    public boolean hasMacros() {
        return protein.isPresent() || carbs.isPresent() || fat.isPresent();
    }
}
