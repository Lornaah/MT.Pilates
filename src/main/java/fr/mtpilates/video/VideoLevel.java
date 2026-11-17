package fr.mtpilates.video;

import java.util.Arrays;
import java.util.Optional;

/** Niveau d'une séance (CHECK level) ; libellés fixes des filtres et du formulaire de l'original. */
public enum VideoLevel {
    BEGINNER("beginner", "Débutante"),
    INTERMEDIATE("intermediate", "Intermédiaire"),
    ADVANCED("advanced", "Avancée");

    private final String code;
    private final String label;

    VideoLevel(final String code, final String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Optional<VideoLevel> fromCode(final String code) {
        return Arrays.stream(values()).filter(l -> l.code.equals(code)).findFirst();
    }
}
