package fr.mtpilates.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Document propre à un membre (table member_documents). */
@Entity
@Table(name = "member_documents")
public class MemberDocument {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(nullable = false)
    private String title;

    private String category;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected MemberDocument() {
    }

    public MemberDocument(final UUID userId, final UUID uploadedBy, final String title, final String category,
                          final String fileUrl, final long fileSizeBytes, final String mimeType, final Instant uploadedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.uploadedBy = uploadedBy;
        this.title = title;
        this.category = category;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Optional<Long> getFileSizeBytes() {
        return Optional.ofNullable(fileSizeBytes);
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
