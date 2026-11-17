package fr.mtpilates.training;

import java.util.Arrays;
import java.util.Optional;

/** Statut d'une planification (PROGRAM_STATUS_META). */
public enum ProgramStatus {
    DRAFT("draft", "Brouillon", "bg-muted text-muted-foreground border-border"),
    SCHEDULED("scheduled", "Programmée", "bg-setter/15 text-setter border-setter/30"),
    ACTIVE("active", "En cours", "bg-coach/15 text-coach border-coach/30"),
    FINISHED("finished", "Terminée", "bg-sage/20 text-sage-foreground border-sage/40"),
    TEMPLATE("template", "Modèle", "bg-trial/15 text-trial border-trial/30");

    private final String code;
    private final String label;
    private final String badgeClass;

    ProgramStatus(final String code, final String label, final String badgeClass) {
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

    public static ProgramStatus of(final String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(DRAFT);
    }

    public static Optional<ProgramStatus> fromCode(final String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst();
    }
}
