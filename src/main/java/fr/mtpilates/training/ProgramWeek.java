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

/** Semaine d'une planification (table program_weeks). */
@Entity
@Table(name = "program_weeks")
public class ProgramWeek {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "week_comment")
    private String comment;

    @OneToMany(mappedBy = "week", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek")
    private List<ProgramDay> days = new ArrayList<>();

    protected ProgramWeek() {
    }

    ProgramWeek(final Program program, final int weekNumber, final String comment) {
        this.id = UUID.randomUUID();
        this.program = program;
        this.weekNumber = weekNumber;
        this.comment = comment;
    }

    public void addDay(final int dayOfWeek, final String dayType, final String dayComment,
                       final WorkoutSession legacySession, final List<WorkoutSession> sessions) {
        final ProgramDay day = new ProgramDay(this, dayOfWeek, dayType, dayComment, legacySession);
        sessions.forEach(day::addSession);
        days.add(day);
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public List<ProgramDay> getDays() {
        return List.copyOf(days);
    }
}
