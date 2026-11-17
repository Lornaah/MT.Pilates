package fr.mtpilates.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Lecture d'une annonce par un utilisateur (table announcement_reads). */
@Entity
@Table(name = "announcement_reads")
public class AnnouncementRead {

    @Id
    private UUID id;

    @Column(name = "announcement_id", nullable = false)
    private UUID announcementId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected AnnouncementRead() {
    }

    public AnnouncementRead(final UUID announcementId, final UUID userId, final Instant readAt) {
        this.id = UUID.randomUUID();
        this.announcementId = announcementId;
        this.userId = userId;
        this.readAt = readAt;
    }

    public UUID getAnnouncementId() {
        return announcementId;
    }
}
