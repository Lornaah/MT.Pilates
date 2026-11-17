package fr.mtpilates.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Jour marqué « fait » par un membre (table program_day_completions). */
@Entity
@Table(name = "program_day_completions")
public class ProgramDayCompletion {

    @Id
    private UUID id;

    @Column(name = "program_day_id", nullable = false)
    private UUID programDayId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected ProgramDayCompletion() {
    }

    public ProgramDayCompletion(final UUID programDayId, final UUID userId, final Instant completedAt) {
        this.id = UUID.randomUUID();
        this.programDayId = programDayId;
        this.userId = userId;
        this.completedAt = completedAt;
    }

    public UUID getProgramDayId() {
        return programDayId;
    }
}
