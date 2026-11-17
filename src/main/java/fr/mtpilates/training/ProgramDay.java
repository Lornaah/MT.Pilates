package fr.mtpilates.training;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Jour d'une semaine (table program_days). Les séances du jour sont dans program_day_sessions ;
 * workout_session_id garde la première, comme l'original (compatibilité).
 */
@Entity
@Table(name = "program_days")
public class ProgramDay {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_week_id", nullable = false)
    private ProgramWeek week;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "day_type", nullable = false)
    private String dayType;

    @Column(name = "day_comment")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id")
    private WorkoutSession legacySession;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ProgramDaySession> sessions = new ArrayList<>();

    protected ProgramDay() {
    }

    ProgramDay(final ProgramWeek week, final int dayOfWeek, final String dayType, final String comment,
               final WorkoutSession legacySession) {
        this.id = UUID.randomUUID();
        this.week = week;
        this.dayOfWeek = dayOfWeek;
        this.dayType = dayType;
        this.comment = comment;
        this.legacySession = legacySession;
    }

    void addSession(final WorkoutSession session) {
        sessions.add(new ProgramDaySession(this, session, sessions.size()));
    }

    public UUID getId() {
        return id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDayType() {
        return dayType;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public Optional<WorkoutSession> getLegacySession() {
        return Optional.ofNullable(legacySession);
    }

    /** Séances du jour dans l'ordre ; à défaut, l'ancienne séance unique. */
    public List<WorkoutSession> getSessions() {
        final List<WorkoutSession> linked = sessions.stream().map(ProgramDaySession::getSession).toList();
        return linked.isEmpty() ? getLegacySession().map(List::of).orElse(List.of()) : linked;
    }
}
