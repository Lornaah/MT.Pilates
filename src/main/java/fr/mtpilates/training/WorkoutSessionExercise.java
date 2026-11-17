package fr.mtpilates.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Exercice placé dans un bloc, avec ses attributs et séries propres (table workout_session_exercises). */
@Entity
@Table(name = "workout_session_exercises")
public class WorkoutSessionExercise {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private WorkoutBlock block;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "position_in_block", nullable = false)
    private int positionInBlock;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> attributes = new ArrayList<>();

    @Column(nullable = false)
    private int sets;

    @Column(name = "exercise_note")
    private String note;

    protected WorkoutSessionExercise() {
    }

    WorkoutSessionExercise(final WorkoutBlock block, final Exercise exercise, final int positionInBlock,
                           final List<Map<String, Object>> attributes, final int sets, final String note) {
        this.id = UUID.randomUUID();
        this.block = block;
        this.exercise = exercise;
        this.positionInBlock = positionInBlock;
        this.attributes = new ArrayList<>(attributes);
        this.sets = sets;
        this.note = note;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public List<Map<String, Object>> getAttributes() {
        return attributes == null ? List.of() : List.copyOf(attributes);
    }

    public int getSets() {
        return sets;
    }

    public Optional<String> getNote() {
        return Optional.ofNullable(note);
    }
}
