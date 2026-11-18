package fr.mtpilates.bilan;

import fr.mtpilates.tracking.Measure;
import java.util.Arrays;
import java.util.Optional;

/** Mesures reprises dans le bilan (METRIC_KEYS de computeBilan), avec leur nom dans le modèle de textes. */
public enum BilanMetric {
    WEIGHT(Measure.WEIGHT, "weight_kg", "poids", "Poids", "kg"),
    WAIST(Measure.WAIST, "waist_cm", "taille", "Tour de taille", "cm"),
    HIP(Measure.HIP, "hip_cm", "hanches", "Hanches", "cm"),
    CHEST(Measure.CHEST, "chest_cm", "poitrine", "Poitrine", "cm"),
    ARM(Measure.ARM, "arm_cm", "bras", "Bras", "cm"),
    THIGH(Measure.THIGH, "thigh_cm", "cuisses", "Cuisses", "cm");

    private final Measure measure;
    private final String key;
    private final String slug;
    private final String label;
    private final String unit;

    BilanMetric(final Measure measure, final String key, final String slug, final String label, final String unit) {
        this.measure = measure;
        this.key = key;
        this.slug = slug;
        this.label = label;
        this.unit = unit;
    }

    public Measure measure() {
        return measure;
    }

    /** Clé de l'original (weight_kg…), conservée dans les instantanés JSON. */
    public String key() {
        return key;
    }

    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }

    public String unit() {
        return unit;
    }

    public static Optional<BilanMetric> fromKey(final String key) {
        return Arrays.stream(values()).filter(m -> m.key.equals(key)).findFirst();
    }

    public static Optional<BilanMetric> fromSlug(final String slug) {
        return Arrays.stream(values()).filter(m -> m.slug.equals(slug)).findFirst();
    }
}
