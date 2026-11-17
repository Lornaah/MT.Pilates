package fr.mtpilates.habit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Habitude cochée un jour donné (table habit_completions). */
@Entity
@Table(name = "habit_completions")
public class HabitCompletion {

    @Id
    private UUID id;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    protected HabitCompletion() {
    }

    public HabitCompletion(final UUID habitId, final UUID userId, final LocalDate completedDate) {
        this.id = UUID.randomUUID();
        this.habitId = habitId;
        this.userId = userId;
        this.completedDate = completedDate;
    }

    public UUID getHabitId() {
        return habitId;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }
}
