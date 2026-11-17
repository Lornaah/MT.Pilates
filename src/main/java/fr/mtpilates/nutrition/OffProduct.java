package fr.mtpilates.nutrition;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Produit OpenFoodFacts normalisé comme le faisait la fonction edge `openfoodfacts`. */
public record OffProduct(Optional<String> barcode, String name, Optional<String> brand, Optional<String> imageUrl,
                         Optional<BigDecimal> servingSizeG, Optional<BigDecimal> calories, Optional<BigDecimal> protein,
                         Optional<BigDecimal> carbs, Optional<BigDecimal> fat, Optional<BigDecimal> fiber) {

    /** Valeurs du formulaire « Nouvel aliment » pré-rempli (offToInsert + formulaire de l'original). */
    public FoodFormValues toForm() {
        return FoodFormValues.imported(new FoodForm(name, brand.orElse(""), barcode.orElse(""), text(servingSizeG),
                text(calories), text(protein), text(carbs), text(fat), text(fiber), null, null, null));
    }

    /** Champs renvoyés par un clic sur un résultat de recherche (paramètres de /foods/prefill). */
    public Map<String, String> prefill() {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("brand", brand.orElse(""));
        values.put("barcode", barcode.orElse(""));
        values.put("servingSizeG", text(servingSizeG));
        values.put("calories", text(calories));
        values.put("protein", text(protein));
        values.put("carbs", text(carbs));
        values.put("fat", text(fat));
        values.put("fiber", text(fiber));
        return values;
    }

    private static String text(final Optional<BigDecimal> value) {
        return value.map(FoodFormValues::plain).orElse("");
    }
}
