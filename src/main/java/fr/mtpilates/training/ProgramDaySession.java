package fr.mtpilates.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Séance placée dans un jour, dans l'ordre d'affichage (table program_day_sessions). */
@Entity
@Table(name = "program_day_sessions")
public class ProgramDaySession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_day_id", nullable = false)
    private ProgramDay day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession session;

    @Column(nullable = false)
    private int position;

    protected ProgramDaySession() {
    }

    ProgramDaySession(final ProgramDay day, final WorkoutSession session, final int position) {
        this.id = UUID.randomUUID();
        this.day = day;
        this.session = session;
        this.position = position;
    }

    public WorkoutSession getSession() {
        return session;
    }
}
