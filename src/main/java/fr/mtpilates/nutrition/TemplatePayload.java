package fr.mtpilates.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** État de l'éditeur de menu type envoyé en JSON (mêmes clés que MealPlanTemplateInsert). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplatePayload(String title, String description, @JsonProperty("cover_color") String coverColor,
                              @JsonProperty("kcal_target") Integer kcalTarget,
                              @JsonProperty("macros_target") Map<String, Object> macrosTarget,
                              @JsonProperty("meals_per_day") Integer mealsPerDay,
                              @JsonProperty("dietary_preferences") Map<String, Object> dietaryPreferences,
                              List<Map<String, Object>> weeks) {
}
