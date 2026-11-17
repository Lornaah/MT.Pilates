package fr.mtpilates.tracking;

/** Mesures du suivi (FIELDS de MyTracking) : nom de paramètre de formulaire, libellé, unité. */
public enum Measure {
    WEIGHT("weightKg", "Poids (kg)", "kg"),
    BODY_FAT("bodyFatPct", "Masse grasse (%)", "%"),
    WAIST("waistCm", "Tour de taille (cm)", "cm"),
    HIP("hipCm", "Tour de hanches (cm)", "cm"),
    CHEST("chestCm", "Tour de poitrine (cm)", "cm"),
    ARM("armCm", "Tour de bras (cm)", "cm"),
    THIGH("thighCm", "Tour de cuisse (cm)", "cm");

    private final String param;
    private final String label;
    private final String unit;

    Measure(final String param, final String label, final String unit) {
        this.param = param;
        this.label = label;
        this.unit = unit;
    }

    public String param() {
        return param;
    }

    public String label() {
        return label;
    }

    public String unit() {
        return unit;
    }
}
