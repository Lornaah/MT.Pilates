package fr.mtpilates.nutrition;

import java.util.List;

/** Palette des couvertures (TEMPLATE_COVERS) ; les couleurs sont des variables CSS --cover-<clé>-bg/-text. */
public final class TemplateCovers {

    public record Cover(String key, String label) {

        public String background() {
            return "var(--cover-" + key + "-bg)";
        }

        public String text() {
            return "var(--cover-" + key + "-text)";
        }
    }

    public static final List<Cover> ALL = List.of(new Cover("teal", "Émeraude"), new Cover("purple", "Iris"),
            new Cover("coral", "Corail"), new Cover("amber", "Ambre"), new Cover("blue", "Bleu"), new Cover("pink", "Rose"));

    private TemplateCovers() {
    }

    /** Clé inconnue ou absente : première couverture, comme coverFor(). */
    public static Cover of(final String key) {
        return ALL.stream().filter(c -> c.key().equals(key)).findFirst().orElse(ALL.get(0));
    }
}
