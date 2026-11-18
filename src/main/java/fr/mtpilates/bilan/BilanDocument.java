package fr.mtpilates.bilan;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tout ce que le gabarit PDF affiche, déjà calculé et mis en forme : textes (XHTML), tuiles de chiffres, pages photo,
 * graphiques (data URI) et progression vers l'objectif.
 *
 * @param html   texte mis en forme par clé du modèle
 * @param points points mis en forme par clé (« vue.dos.points »…)
 * @param hero   photo d'illustration (une photo « après » de la membre), si elle en a envoyé
 */
public record BilanDocument(Map<String, String> html, Map<String, List<Point>> points, List<Tile> tiles,
                            List<ViewPage> views, Optional<String> weightChart, List<MeasurePage> measures,
                            Optional<Objective> objective, Optional<String> hero) {

    public record Point(String title, String text) {
    }

    /** Tuile « Tes résultats en chiffres » : écart, intitulé, légende. */
    public record Tile(String value, String label, String caption) {
    }

    /** Page photo d'une vue (dos, profil, face) : photos avant et après annotées. */
    public record ViewPage(String slug, String before, String after) {
    }

    /** Page d'une mensuration : écart affiché et graphique. */
    public record MeasurePage(String slug, String delta, String chart) {
    }

    /** Barre de progression vers le poids objectif. */
    public record Objective(String percentLabel, double percent) {
    }

    public String text(final String key) {
        return html.getOrDefault(key, "");
    }

    public boolean has(final String key) {
        return !text(key).isBlank();
    }

    public List<Point> pointsOf(final String key) {
        return points.getOrDefault(key, List.of());
    }
}
