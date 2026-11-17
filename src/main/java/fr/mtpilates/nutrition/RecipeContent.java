package fr.mtpilates.nutrition;

import java.math.BigDecimal;
import java.util.List;

/** Champs éditables d'une recette (RecipeFormDialog). */
public record RecipeContent(String title, String description, String category, Integer calories, BigDecimal protein,
                            BigDecimal carbs, BigDecimal fat, String instructions, List<Ingredient> ingredients,
                            String imageUrl, Integer prepMinutes, Integer cookMinutes, int servings,
                            List<String> dietaryTags, List<String> allergens, List<String> equipment,
                            List<String> mealTypes, boolean dessert) {
}
