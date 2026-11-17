package fr.mtpilates.tracking;

/** « Nouvelle entrée de suivi » : valeurs saisies telles quelles (vides = non mesurées). */
public record TrackingEntryForm(String date, String notes, String weightKg, String bodyFatPct, String waistCm,
                                String hipCm, String chestCm, String armCm, String thighCm) {
}
