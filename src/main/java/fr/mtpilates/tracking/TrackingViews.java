package fr.mtpilates.tracking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues du suivi (cartes de mensurations, bilans hebdomadaires, photos). */
public final class TrackingViews {

    private TrackingViews() {
    }

    /** Tendance d'une mesure par rapport à l'entrée précédente : une baisse est un progrès. */
    public enum Trend { DOWN, UP, SAME, NONE }

    public record MeasureValue(String label, String value, String unit, Trend trend) {
    }

    public record EntryView(UUID id, String dateLabel, List<MeasureValue> values, Optional<String> notes) {
    }

    public record ReviewView(UUID id, String weekLabel, int wellbeing, Optional<String> note) {
    }

    public record PhotoView(UUID id, String url, String sideLabel, String takenAtIso, String takenAtLabel,
                            Optional<String> notes) {
    }
}
