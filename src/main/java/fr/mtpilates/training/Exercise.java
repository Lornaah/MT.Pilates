package fr.mtpilates.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Exercice de la bibliothèque (table exercises). */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "demo_url")
    private String demoUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "muscle_groups", columnDefinition = "text[]", nullable = false)
    private List<String> muscleGroups = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> equipment = new ArrayList<>();

    @Column(name = "exercise_type")
    private String exerciseType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_attributes", nullable = false)
    private List<Map<String, Object>> defaultAttributes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> objectives = new ArrayList<>();

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Exercise() {
    }

    public Exercise(final ExerciseContent content, final UUID createdBy) {
        this.id = UUID.randomUUID();
        apply(content, createdBy);
    }

    /**
     * L'original réécrivait aussi le favori (à faux) et l'auteur (l'utilisateur courant) à chaque enregistrement ;
     * ce comportement est conservé (décision Q2 : seuls les bugs bloquants sont corrigés).
     */
    public void apply(final ExerciseContent content, final UUID editor) {
        this.title = content.title();
        this.description = content.description();
        this.videoUrl = content.videoUrl();
        this.demoUrl = content.demoUrl();
        this.thumbnailUrl = content.thumbnailUrl();
        this.muscleGroups = new ArrayList<>(content.muscleGroups());
        this.equipment = new ArrayList<>(content.equipment());
        this.exerciseType = content.exerciseType();
        this.objectives = new ArrayList<>(content.objectives());
        this.defaultAttributes = new ArrayList<>(content.defaultAttributes());
        this.published = content.published();
        this.favorite = false;
        this.createdBy = editor;
    }

    /** Copie « (copie) » de duplicate_exercise : objectifs non repris, favori retiré, auteur = qui duplique. */
    public Exercise duplicate(final UUID by) {
        final Exercise copy = new Exercise(new ExerciseContent(title + " (copie)", description, videoUrl, demoUrl,
                thumbnailUrl, muscleGroups, equipment, exerciseType, List.of(), defaultAttributes, published), by);
        copy.favorite = false;
        return copy;
    }

    public void toggleFavorite() {
        this.favorite = !favorite;
    }

    public UUID getId() {
        return id;
    }

    public ExerciseContent content() {
        return new ExerciseContent(title, description, videoUrl, demoUrl, thumbnailUrl, List.copyOf(muscleGroups),
                List.copyOf(equipment), exerciseType, List.copyOf(objectives),
                defaultAttributes == null ? List.of() : List.copyOf(defaultAttributes), published);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public boolean isPublished() {
        return published;
    }

    public Optional<UUID> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }
}
