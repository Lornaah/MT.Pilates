package fr.mtpilates.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Photo d'évolution (table member_photos). */
@Entity
@Table(name = "member_photos")
public class MemberPhoto {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "taken_at", nullable = false)
    private LocalDate takenAt;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(nullable = false)
    private String side;

    private String notes;

    protected MemberPhoto() {
    }

    public MemberPhoto(final UUID userId, final LocalDate takenAt, final String photoUrl, final PhotoSide side,
                       final String notes) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.takenAt = takenAt;
        this.photoUrl = photoUrl;
        this.side = side.code();
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getTakenAt() {
        return takenAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public PhotoSide getSide() {
        return PhotoSide.fromCode(side).orElse(PhotoSide.FRONT);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }
}
