package fr.mtpilates.training;

import java.util.Arrays;
import java.util.Optional;

/** Niveau d'une séance, avec la pastille colorée de la bibliothèque. */
public enum SessionLevel {
    BEGINNER("beginner", "Débutant", "bg-sage/20 text-sage-foreground border-sage/40"),
    INTERMEDIATE("intermediate", "Intermédiaire", "bg-coach/15 text-coach border-coach/30"),
    ADVANCED("advanced", "Avancé", "bg-bordeaux/15 text-bordeaux border-bordeaux/30");

    private final String code;
    private final String label;
    private final String badgeClass;

    SessionLevel(final String code, final String label, final String badgeClass) {
        this.code = code;
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String badgeClass() {
        return badgeClass;
    }

    public static Optional<SessionLevel> fromCode(final String code) {
        return Arrays.stream(values()).filter(l -> l.code.equals(code)).findFirst();
    }
}
