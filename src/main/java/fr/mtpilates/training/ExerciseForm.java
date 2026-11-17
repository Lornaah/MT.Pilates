package fr.mtpilates.training;

import java.util.List;

/** Formulaire « Nouvel exercice » / « Modifier l'exercice » ; attributs en JSON (éditeur Alpine). */
public record ExerciseForm(String title, String description, String videoUrl, String demoUrl, String thumbnailUrl,
                           List<String> muscleGroups, List<String> equipment, String exerciseType,
                           List<String> objectives, String defaultAttributes, String published) {
}
