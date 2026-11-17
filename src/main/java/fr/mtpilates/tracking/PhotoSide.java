package fr.mtpilates.tracking;

import java.util.Arrays;
import java.util.Optional;

/** Vue d'une photo d'évolution (CHECK side). */
public enum PhotoSide {
    FRONT("front", "Face"),
    SIDE("side", "Profil"),
    BACK("back", "Dos");

    private final String code;
    private final String label;

    PhotoSide(final String code, final String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Optional<PhotoSide> fromCode(final String code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst();
    }
}
