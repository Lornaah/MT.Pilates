package fr.mtpilates.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** État du RecipeFormDialog envoyé en JSON (mêmes clés que le FormState de l'original, nombres en texte). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipePayload(String title, String description, String category, String calories,
                            @JsonProperty("protein_g") String protein, @JsonProperty("carbs_g") String carbs,
                            @JsonProperty("fat_g") String fat, String instructions, List<Line> ingredients,
                            @JsonProperty("image_url") String imageUrl,
                            @JsonProperty("prep_time_minutes") String prepMinutes,
                            @JsonProperty("cook_time_minutes") String cookMinutes, String servings,
                            @JsonProperty("dietary_tags") List<String> dietaryTags, List<String> allergens,
                            @JsonProperty("equipment_needed") List<String> equipment,
                            @JsonProperty("meal_types") List<String> mealTypes,
                            @JsonProperty("is_dessert") boolean dessert) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(String name, String quantity, String unit) {
    }
}
