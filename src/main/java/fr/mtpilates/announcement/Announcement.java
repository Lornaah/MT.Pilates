package fr.mtpilates.announcement;

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

/** Annonce aux membres (table announcements) ; une date de publication en 2099 ou après vaut brouillon. */
@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(nullable = false)
    private String channel = "in_app";

    @Column(nullable = false)
    private String target = "all";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "target_user_ids", columnDefinition = "uuid[]", nullable = false)
    private List<UUID> targetUserIds = new ArrayList<>();

    @Column(name = "target_coach_id")
    private UUID targetCoachId;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "recurrence_rule")
    private String recurrenceRule;

    @Column(name = "publish_at", nullable = false)
    private Instant publishAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected Announcement() {
    }

    public Announcement(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    /** L'original réécrivait aussi l'auteur à chaque enregistrement. */
    public void update(final AnnouncementContent c, final UUID editor) {
        this.title = c.title();
        this.body = c.body();
        this.coverImageUrl = c.coverImageUrl();
        this.channel = c.channel();
        this.target = c.target();
        this.targetUserIds = new ArrayList<>(c.targetUserIds());
        this.targetCoachId = c.targetCoachId();
        this.recurring = c.recurring();
        this.recurrenceRule = c.recurrenceRule();
        this.publishAt = c.publishAt();
        this.createdBy = editor;
    }

    public UUID getId() {
        return id;
    }

    public AnnouncementContent content() {
        return new AnnouncementContent(title, body, coverImageUrl, channel, target,
                targetUserIds == null ? List.of() : List.copyOf(targetUserIds), targetCoachId, recurring, recurrenceRule,
                publishAt);
    }

    public Instant getPublishAt() {
        return publishAt;
    }

    public Optional<String> getCoverImageUrl() {
        return Optional.ofNullable(coverImageUrl).filter(s -> !s.isEmpty());
    }
}
