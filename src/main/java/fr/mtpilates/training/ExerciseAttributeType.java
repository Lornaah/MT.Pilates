package fr.mtpilates.training;

import java.util.Arrays;
import java.util.Optional;

/** Les 14 types d'attributs d'exercice (EXERCISE_ATTRIBUTE_META) : libellé, unité, nature de la valeur, défaut. */
public enum ExerciseAttributeType {
    WEIGHT("weight", "Charge", "kg", Kind.NUMBER, 20),
    BODYWEIGHT("bodyweight", "Poids du corps", null, Kind.BOOLEAN, true),
    REPS("reps", "Répétitions", null, Kind.NUMBER, 10),
    TIME("time", "Durée", "s", Kind.NUMBER, 30),
    TEMPO("tempo", "Tempo (EPCP)", null, Kind.STRING, "2-1-2-1"),
    REPS_RANGE("reps_range", "Plage de reps", null, Kind.STRING, "8-12"),
    AMRAP("amrap", "AMRAP", null, Kind.BOOLEAN, true),
    REST("rest", "Récupération", "s", Kind.NUMBER, 60),
    KCAL("kcal", "Calories", "kcal", Kind.NUMBER, 100),
    METERS("meters", "Distance", "m", Kind.NUMBER, 500),
    RPE("rpe", "RPE (Borg 1–10)", null, Kind.NUMBER, 7),
    MAX("max", "Plafond AMRAP", null, Kind.NUMBER, 20),
    LEVEL("level", "Niveau (machine)", null, Kind.NUMBER, 5),
    NA("na", "Non applicable", null, Kind.NONE, null);

    /** Nature de la valeur saisie. */
    public enum Kind { NUMBER, STRING, BOOLEAN, NONE }

    public static final int MAX_PER_EXERCISE = 5;

    private final String code;
    private final String label;
    private final String unit;
    private final Kind kind;
    private final Object defaultValue;

    ExerciseAttributeType(final String code, final String label, final String unit, final Kind kind,
                          final Object defaultValue) {
        this.code = code;
        this.label = label;
        this.unit = unit;
        this.kind = kind;
        this.defaultValue = defaultValue;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public Optional<String> unit() {
        return Optional.ofNullable(unit);
    }

    public Kind kind() {
        return kind;
    }

    public Optional<Object> defaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public static Optional<ExerciseAttributeType> fromCode(final String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }
}
