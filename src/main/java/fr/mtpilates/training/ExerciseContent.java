package fr.mtpilates.training;

import java.util.List;
import java.util.Map;

/** Contenu éditable d'un exercice ; les champs texte vides valent null, comme l'original. */
public record ExerciseContent(String title, String description, String videoUrl, String demoUrl, String thumbnailUrl,
                              List<String> muscleGroups, List<String> equipment, String exerciseType,
                              List<String> objectives, List<Map<String, Object>> defaultAttributes, boolean published) {
}
