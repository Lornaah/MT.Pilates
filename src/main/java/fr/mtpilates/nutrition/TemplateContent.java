package fr.mtpilates.nutrition;

import java.util.List;
import java.util.Map;

/** Contenu enregistré d'un menu type ; les semaines restent au format JSON de l'original. */
public record TemplateContent(String title, String description, String status, String coverColor, Integer kcalTarget,
                              Map<String, Object> macrosTarget, int mealsPerDay, Map<String, Object> dietaryPreferences,
                              List<Map<String, Object>> weeks) {
}
