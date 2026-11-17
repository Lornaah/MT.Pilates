package fr.mtpilates.document;

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

/** Document de la bibliothèque partagée (table documents) ; file_url est le chemin dans le bucket privé. */
@Entity
@Table(name = "documents")
public class LibraryDocument {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> tags = new ArrayList<>();

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Integer fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(nullable = false)
    private String visibility = "staff_only";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "shared_with_ids", columnDefinition = "uuid[]", nullable = false)
    private List<UUID> sharedWithIds = new ArrayList<>();

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected LibraryDocument() {
    }

    public LibraryDocument(final UUID uploadedBy) {
        this.id = UUID.randomUUID();
        this.uploadedBy = uploadedBy;
    }

    /** L'original réécrivait aussi l'auteur à chaque enregistrement. */
    public void update(final DocumentContent c, final UUID editor) {
        this.title = c.title();
        this.description = c.description();
        this.category = c.category();
        this.tags = new ArrayList<>(c.tags());
        this.visibility = c.visibility();
        this.sharedWithIds = new ArrayList<>(c.sharedWithIds());
        this.fileUrl = c.fileUrl();
        this.fileSizeBytes = c.fileSizeBytes();
        this.mimeType = c.mimeType();
        this.uploadedBy = editor;
    }

    public UUID getId() {
        return id;
    }

    public DocumentContent content() {
        return new DocumentContent(title, description, category, tags == null ? List.of() : List.copyOf(tags), visibility,
                sharedWithIds == null ? List.of() : List.copyOf(sharedWithIds), fileUrl, fileSizeBytes, mimeType);
    }

    /** Règle de lecture de l'original : staff, ou document ouvert à tous, ou membre de la liste. */
    public boolean readableBy(final UUID userId, final boolean staff) {
        return staff || "all_members".equals(visibility)
                || ("specific".equals(visibility) && sharedWithIds != null && sharedWithIds.contains(userId));
    }

    public Optional<String> getMimeType() {
        return Optional.ofNullable(mimeType);
    }
}
