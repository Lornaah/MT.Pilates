package fr.mtpilates.nutrition;

import java.util.List;

/** Carnet de recettes : cartes et compteurs des tuiles d'en-tête. */
public record RecipeLibrary(List<RecipeCard> recipes, long mine, long withPhoto) {
}
