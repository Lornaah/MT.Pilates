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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Bloc d'une séance : exercice seul, superset, triset ou circuit (table workout_session_blocks). */
@Entity
@Table(name = "workout_session_blocks")
public class WorkoutBlock {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @Column(nullable = false)
    private int position;

    @Column(name = "block_type", nullable = false)
    private String blockType;

    @Column(nullable = false)
    private int rounds;

    private String notes;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionInBlock")
    private List<WorkoutSessionExercise> exercises = new ArrayList<>();

    protected WorkoutBlock() {
    }

    WorkoutBlock(final WorkoutSession session, final int position, final String blockType, final int rounds,
                 final String notes) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.position = position;
        this.blockType = blockType;
        this.rounds = rounds;
        this.notes = notes;
    }

    public void addExercise(final Exercise exercise, final List<Map<String, Object>> attributes, final int sets,
                            final String note) {
        exercises.add(new WorkoutSessionExercise(this, exercise, exercises.size(), attributes, sets, note));
    }

    public String getBlockType() {
        return blockType;
    }

    public int getRounds() {
        return rounds;
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    public List<WorkoutSessionExercise> getExercises() {
        return List.copyOf(exercises);
    }
}
