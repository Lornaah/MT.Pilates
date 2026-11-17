package fr.mtpilates.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Bilan hebdomadaire du membre (table weekly_reviews), unique par semaine. */
@Entity
@Table(name = "weekly_reviews")
public class WeeklyReview {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "mental_wellbeing", nullable = false)
    private int mentalWellbeing;

    @Column(name = "free_note")
    private String freeNote;

    protected WeeklyReview() {
    }

    public WeeklyReview(final UUID userId, final LocalDate weekStartDate) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.weekStartDate = weekStartDate;
    }

    public void record(final int wellbeing, final String note) {
        this.mentalWellbeing = wellbeing;
        this.freeNote = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public int getMentalWellbeing() {
        return mentalWellbeing;
    }

    public Optional<String> getFreeNote() {
        return Optional.ofNullable(freeNote);
    }
}
