package fr.mtpilates.training;

import java.util.List;

/** Bibliothèque : cartes, compteurs des onglets et listes de filtres (suggestions + valeurs utilisées). */
public record ExerciseLibraryView(List<ExerciseCard> exercises, long mine, long favorites, List<String> muscleOptions,
                                  List<String> equipmentOptions, List<String> typeOptions, List<String> objectiveOptions) {
}
