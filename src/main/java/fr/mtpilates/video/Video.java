package fr.mtpilates.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Séance vidéo YouTube (table videos). */
@Entity
@Table(name = "videos")
public class Video {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "youtube_id", nullable = false)
    private String youtubeId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    private String level;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "focus_areas", columnDefinition = "text[]")
    private List<String> focusAreas = new ArrayList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_published")
    private Boolean published = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Video() {
    }

    public Video(final String title, final String description, final String youtubeId, final Integer durationMinutes,
                 final VideoLevel level, final List<String> focusAreas, final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.youtubeId = youtubeId;
        this.durationMinutes = durationMinutes;
        this.level = level.code();
        this.focusAreas = new ArrayList<>(focusAreas);
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public Optional<String> getThumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl).filter(url -> !url.isEmpty());
    }

    public Optional<Integer> getDurationMinutes() {
        return Optional.ofNullable(durationMinutes);
    }

    public String getLevel() {
        return Optional.ofNullable(level).orElse("");
    }

    public List<String> getFocusAreas() {
        return focusAreas == null ? List.of() : List.copyOf(focusAreas);
    }
}
