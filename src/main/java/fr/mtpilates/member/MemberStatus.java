package fr.mtpilates.member;

import java.util.Arrays;
import java.util.Optional;

/** Cycle de vie d'un membre (statusConfig.ts de l'original) : libellé fixe et variante de badge. */
public enum MemberStatus {
    ACTIVE("active", "Actif", "success"),
    PAUSED("paused", "En pause", "warm"),
    ARCHIVED("archived", "Archivé", "secondary"),
    FINISHED("finished", "Terminé", "outline");

    private final String code;
    private final String label;
    private final String variant;

    MemberStatus(final String code, final String label, final String variant) {
        this.code = code;
        this.label = label;
        this.variant = variant;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String variant() {
        return variant;
    }

    public static Optional<MemberStatus> fromCode(final String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst();
    }
}
