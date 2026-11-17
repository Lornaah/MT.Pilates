package fr.mtpilates.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Carte de la bibliothèque d'exercices, avec les données utiles au filtrage côté navigateur. */
public record ExerciseCard(UUID id, String title, Optional<String> thumbnail, List<String> muscleGroups,
                           List<String> equipment, List<String> objectives, String type, boolean favorite,
                           boolean mine) {
}
