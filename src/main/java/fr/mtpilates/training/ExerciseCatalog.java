package fr.mtpilates.training;

import java.util.List;

/** Suggestions fixes de l'original (types/exercise.ts). */
public final class ExerciseCatalog {

    public static final List<String> MUSCLE_GROUPS = List.of(
            "Pectoraux", "Dos", "Jambes", "Quadriceps", "Ischio-jambiers", "Mollets",
            "Fessiers", "Épaules", "Biceps", "Triceps", "Avant-bras", "Abdominaux",
            "Lombaires", "Trapèzes", "Corps entier");

    public static final List<String> EQUIPMENT = List.of(
            "Aucun (poids du corps)", "Haltères", "Barre", "Kettlebell", "Élastiques",
            "TRX / Sangles", "Banc", "Tapis", "Swiss ball", "Medicine ball",
            "Poulie", "Machine guidée", "Smith machine", "Box / Step", "Corde à sauter",
            "Vélo", "Rameur", "Tapis de course", "Barre de traction", "Anneaux");

    public static final List<String> TYPES = List.of("strength", "cardio", "mobility", "flexibility", "plyometric",
            "isometric");

    private ExerciseCatalog() {
    }
}
