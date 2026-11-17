package fr.mtpilates.nutrition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ingrédient d'une recette (élément du jsonb `recipes.ingredients`) ; la quantité est un texte libre. */
public record Ingredient(String name, String quantity, String unit) {

    public static Ingredient of(final Map<String, Object> raw) {
        return new Ingredient(text(raw.get("name")), text(raw.get("quantity")), text(raw.get("unit")));
    }

    public Map<String, Object> toJson() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("unit", unit);
        return map;
    }

    private static String text(final Object value) {
        return value == null ? "" : value.toString();
    }
}
