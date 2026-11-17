package fr.mtpilates.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Entrée de mensurations saisie par le membre (table member_tracking). */
@Entity
@Table(name = "member_tracking")
public class TrackingEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "body_fat_pct")
    private BigDecimal bodyFatPct;

    @Column(name = "waist_cm")
    private BigDecimal waistCm;

    @Column(name = "hip_cm")
    private BigDecimal hipCm;

    @Column(name = "chest_cm")
    private BigDecimal chestCm;

    @Column(name = "arm_cm")
    private BigDecimal armCm;

    @Column(name = "thigh_cm")
    private BigDecimal thighCm;

    private String notes;

    protected TrackingEntry() {
    }

    public TrackingEntry(final UUID userId, final LocalDate date, final Map<Measure, BigDecimal> values,
                         final String notes) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.date = date;
        this.weightKg = values.get(Measure.WEIGHT);
        this.bodyFatPct = values.get(Measure.BODY_FAT);
        this.waistCm = values.get(Measure.WAIST);
        this.hipCm = values.get(Measure.HIP);
        this.chestCm = values.get(Measure.CHEST);
        this.armCm = values.get(Measure.ARM);
        this.thighCm = values.get(Measure.THIGH);
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    /** Valeurs renseignées uniquement, dans l'ordre des mesures. */
    public Map<Measure, BigDecimal> values() {
        final Map<Measure, BigDecimal> values = new EnumMap<>(Measure.class);
        put(values, Measure.WEIGHT, weightKg);
        put(values, Measure.BODY_FAT, bodyFatPct);
        put(values, Measure.WAIST, waistCm);
        put(values, Measure.HIP, hipCm);
        put(values, Measure.CHEST, chestCm);
        put(values, Measure.ARM, armCm);
        put(values, Measure.THIGH, thighCm);
        return values;
    }

    private static void put(final Map<Measure, BigDecimal> values, final Measure measure, final BigDecimal value) {
        if (value != null) {
            values.put(measure, value);
        }
    }
}
