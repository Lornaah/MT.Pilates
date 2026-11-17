package fr.mtpilates.habit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Habitude assignée à un membre (table habit_assignments). */
@Entity
@Table(name = "habit_assignments")
public class HabitAssignment {

    @Id
    private UUID id;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected HabitAssignment() {
    }

    public HabitAssignment(final UUID habitId, final UUID userId) {
        this.id = UUID.randomUUID();
        this.habitId = habitId;
        this.userId = userId;
    }

    public UUID getHabitId() {
        return habitId;
    }

    public UUID getUserId() {
        return userId;
    }
}
