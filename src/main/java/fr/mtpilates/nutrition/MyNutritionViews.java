package fr.mtpilates.nutrition;

import java.util.List;
import java.util.Optional;

/** Vues de « Ton plan nutritionnel » (MyNutrition), pour les deux formats de `meals`. */
public final class MyNutritionViews {

    private MyNutritionViews() {
    }

    public record Plan(String title, Optional<String> description, Optional<String> pdfUrl, boolean showTargets,
                       String kcal, String protein, String carbs, String fat, boolean generated, List<Day> days,
                       List<ShoppingGroup> shopping) {
    }

    public record Day(String label, boolean hasContent, List<PlannedMeal> planned, List<LegacyBlock> legacy) {
    }

    /** Repas généré : macros affichées seulement si le coach ne les masque pas. */
    public record PlannedMeal(String icon, String label, boolean free, String title, String servings,
                              boolean showMacros, Optional<Long> kcal, Optional<Long> protein, Optional<Long> carbs,
                              Optional<Long> fat) {
    }

    /** Repas saisi à la main : texte libre ou liste d'aliments ; ni l'un ni l'autre affiche « — ». */
    public record LegacyBlock(String icon, String label, Optional<String> text, List<LegacyItem> items) {
    }

    public record LegacyItem(Optional<String> quantity, String name, Optional<String> fromRecipe) {
    }

    public record ShoppingGroup(String aisle, List<ShoppingLine> items) {
    }

    public record ShoppingLine(String name, String quantity) {
    }
}
