package fr.mtpilates.nutrition;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Valeurs affichées dans le dialog aliment (texte brut, comme l'état du formulaire de l'original). */
public record FoodFormValues(Optional<UUID> id, String name, String brand, String barcode, String servingSizeG,
                             String calories, String protein, String carbs, String fat, String fiber, String source,
                             String aisle, boolean isPublic) {

    public static FoodFormValues empty() {
        return new FoodFormValues(Optional.empty(), "", "", "", "", "", "", "", "", "", "manual", "autre", false);
    }

    /** Aliment importé d'OpenFoodFacts : source OpenFoodFacts, visible par tous, rayon « Autre ». */
    public static FoodFormValues imported(final FoodForm f) {
        return new FoodFormValues(Optional.empty(), orEmpty(f.name()), orEmpty(f.brand()), orEmpty(f.barcode()),
                orEmpty(f.servingSizeG()), orEmpty(f.calories()), orEmpty(f.protein()), orEmpty(f.carbs()), orEmpty(f.fat()),
                orEmpty(f.fiber()), "open_food_facts", "autre", true);
    }

    public static FoodFormValues of(final UUID id, final FoodContent c) {
        return new FoodFormValues(Optional.of(id), c.name(), orEmpty(c.brand()), orEmpty(c.barcode()), plain(c.servingSizeG()),
                plain(c.calories()), plain(c.protein()), plain(c.carbs()), plain(c.fat()), plain(c.fiber()), c.source(),
                c.aisle() == null ? "autre" : c.aisle(), c.isPublic());
    }

    public String title() {
        return id.isPresent() ? "Modifier l'aliment" : "Nouvel aliment";
    }

    /** Équivalent de `Number.toString()` pour les valeurs numeric renvoyées par l'API de l'original. */
    static String plain(final BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String orEmpty(final String value) {
        return value == null ? "" : value;
    }
}
