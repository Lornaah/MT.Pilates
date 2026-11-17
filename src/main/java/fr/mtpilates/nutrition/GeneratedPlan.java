package fr.mtpilates.nutrition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Plan envoyé par le générateur (CreateMealPlanArgs de l'original). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedPlan(@JsonProperty("memberId") UUID memberId, String title,
                            Map<String, Object> meals, @JsonProperty("shoppingList") List<Map<String, Object>> shoppingList,
                            @JsonProperty("kcalTarget") Integer kcalTarget,
                            @JsonProperty("macrosTarget") Map<String, Object> macrosTarget,
                            @JsonProperty("hideMacros") boolean hideMacros) {
}
