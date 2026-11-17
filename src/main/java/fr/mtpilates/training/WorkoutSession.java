package fr.mtpilates.training;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Séance d'entraînement composée de blocs (table workout_sessions). */
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    private String category;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    private String level;

    @Column(name = "is_template", nullable = false)
    private boolean template;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<WorkoutBlock> blocks = new ArrayList<>();

    protected WorkoutSession() {
    }

    public WorkoutSession(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    public void describe(final String newTitle, final String newDescription, final String newCategory,
                         final Integer newDuration, final String newLevel, final boolean isTemplate,
                         final boolean isPublished) {
        this.title = newTitle;
        this.description = newDescription;
        this.category = newCategory;
        this.durationMinutes = newDuration;
        this.level = newLevel;
        this.template = isTemplate;
        this.published = isPublished;
    }

    /** Vide les blocs ; l'appelant flushe avant d'en ajouter (positions uniques par séance). */
    public void clearBlocks() {
        blocks.clear();
    }

    public WorkoutBlock addBlock(final String blockType, final int rounds, final String notes) {
        final WorkoutBlock block = new WorkoutBlock(this, blocks.size(), blockType, rounds, notes);
        blocks.add(block);
        return block;
    }

    public WorkoutSession duplicate(final UUID by) {
        final WorkoutSession copy = new WorkoutSession(by);
        copy.describe(title + " (copie)", description, category, durationMinutes, level, template, published);
        for (final WorkoutBlock block : blocks) {
            final WorkoutBlock copied = copy.addBlock(block.getBlockType(), block.getRounds(), block.getNotes().orElse(null));
            block.getExercises().forEach(e -> copied.addExercise(e.getExercise(), e.getAttributes(), e.getSets(),
                    e.getNote().orElse(null)));
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

    public Optional<String> getCategory() {
        return Optional.ofNullable(category).filter(s -> !s.isEmpty());
    }

    public Optional<Integer> getDurationMinutes() {
        return Optional.ofNullable(durationMinutes);
    }

    public Optional<String> getLevel() {
        return Optional.ofNullable(level).filter(s -> !s.isEmpty());
    }

    public boolean isTemplate() {
        return template;
    }

    public boolean isPublished() {
        return published;
    }

    public List<WorkoutBlock> getBlocks() {
        return List.copyOf(blocks);
    }
}
