package fr.mtpilates.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Événement d'agenda (table events). */
@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String type;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    private String location;

    @Column(name = "meeting_url")
    private String meetingUrl;

    @Column(name = "coach_id")
    private UUID coachId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "is_private")
    private Boolean privateEvent;

    @Column(name = "created_by")
    private UUID createdBy;

    protected Event() {
    }

    /** Création depuis le calendrier : privé, d'une heure, rattaché à son auteur (EventFormDialog). */
    public Event(final String title, final String description, final EventType type, final Instant startsAt,
                 final Instant endsAt, final UUID memberId, final UUID author) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.type = type.code();
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.memberId = memberId;
        this.privateEvent = true;
        this.coachId = author;
        this.createdBy = author;
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

    public String getType() {
        return type;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Optional<String> getLocation() {
        return Optional.ofNullable(location).filter(s -> !s.isEmpty());
    }

    public boolean hasMeeting() {
        return meetingUrl != null && !meetingUrl.isEmpty();
    }

    public Optional<UUID> getCoachId() {
        return Optional.ofNullable(coachId);
    }

    public Optional<UUID> getMemberId() {
        return Optional.ofNullable(memberId);
    }

    public Optional<UUID> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public boolean isPrivate() {
        return Boolean.TRUE.equals(privateEvent);
    }
}
