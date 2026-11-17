package fr.mtpilates.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Visionnage d'une séance (table video_views). */
@Entity
@Table(name = "video_views")
public class VideoView {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "watched_at", nullable = false)
    private Instant watchedAt;

    private Boolean completed;

    private Integer rating;

    protected VideoView() {
    }

    public VideoView(final UUID userId, final Video video, final Instant watchedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.video = video;
        this.watchedAt = watchedAt;
        this.completed = false;
    }

    public Video getVideo() {
        return video;
    }

    public Instant getWatchedAt() {
        return watchedAt;
    }

    public boolean isCompleted() {
        return Boolean.TRUE.equals(completed);
    }

    public Optional<Integer> getRating() {
        return Optional.ofNullable(rating);
    }
}
