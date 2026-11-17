package fr.mtpilates.training;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Planification : semaines de 7 jours, pour un élève ou des groupes d'objectifs (table programs). */
@Entity
@Table(name = "programs")
public class Program {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> objectives = new ArrayList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber")
    private List<ProgramWeek> weeks = new ArrayList<>();

    protected Program() {
    }

    public Program(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    /** L'original réécrivait aussi l'auteur à chaque enregistrement. */
    public void describe(final String newTitle, final String newDescription, final String newStatus,
                         final UUID newAssignee, final LocalDate newStart, final Integer newDuration,
                         final List<String> newObjectives, final UUID editor) {
        this.title = newTitle;
        this.description = newDescription;
        this.status = newStatus;
        this.assigneeId = newAssignee;
        this.startDate = newStart;
        this.durationWeeks = newDuration;
        this.objectives = new ArrayList<>(newObjectives);
        this.createdBy = editor;
    }

    public void clearWeeks() {
        weeks.clear();
    }

    public ProgramWeek addWeek(final int weekNumber, final String comment) {
        final ProgramWeek week = new ProgramWeek(this, weekNumber, comment);
        weeks.add(week);
        return week;
    }

    /** duplicate_program : « (copie) », brouillon, sans élève ni date, mêmes semaines et jours. */
    public Program duplicate(final UUID by) {
        final Program copy = new Program(by);
        copy.describe(title + " (copie)", description, "draft", null, null, durationWeeks, objectives, by);
        for (final ProgramWeek week : weeks) {
            final ProgramWeek copied = copy.addWeek(week.getWeekNumber(), week.getComment().orElse(null));
            week.getDays().forEach(d -> copied.addDay(d.getDayOfWeek(), d.getDayType(), d.getComment().orElse(null),
                    d.getLegacySession().orElse(null), d.getSessions()));
        }
        return copy;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description).filter(s -> !s.isEmpty());
    }

    public String getStatus() {
        return status;
    }

    public Optional<UUID> getAssigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public Optional<LocalDate> getStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<Integer> getDurationWeeks() {
        return Optional.ofNullable(durationWeeks);
    }

    public List<String> getObjectives() {
        return objectives == null ? List.of() : List.copyOf(objectives);
    }

    public List<ProgramWeek> getWeeks() {
        return List.copyOf(weeks);
    }
}
