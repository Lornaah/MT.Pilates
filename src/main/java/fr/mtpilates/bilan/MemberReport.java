package fr.mtpilates.bilan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Bilan d'un membre (table member_reports) : brouillon modifiable, puis envoyé avec son PDF figé. */
@Entity
@Table(name = "member_reports")
public class MemberReport {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "target_weight_kg")
    private BigDecimal targetWeightKg;

    @Column(name = "start_weight_override_kg")
    private BigDecimal startWeightOverrideKg;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> snapshot = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> content = new LinkedHashMap<>();

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected MemberReport() {
    }

    public MemberReport(final UUID userId, final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdBy = createdBy;
    }

    public void updateSettings(final LocalDate start, final LocalDate end, final BigDecimal target,
                               final BigDecimal startWeightOverride, final Map<String, Object> newContent) {
        this.periodStart = start;
        this.periodEnd = end;
        this.targetWeightKg = target;
        this.startWeightOverrideKg = startWeightOverride;
        this.content = new LinkedHashMap<>(newContent);
    }

    /** Envoi : l'instantané des chiffres et le PDF sont figés. */
    public void markSent(final Map<String, Object> newSnapshot, final String path, final Instant now) {
        this.snapshot = new LinkedHashMap<>(newSnapshot);
        this.pdfPath = path;
        this.status = "sent";
        this.sentAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Optional<BigDecimal> getTargetWeightKg() {
        return Optional.ofNullable(targetWeightKg);
    }

    public Optional<BigDecimal> getStartWeightOverrideKg() {
        return Optional.ofNullable(startWeightOverrideKg);
    }

    public Map<String, Object> getContent() {
        return content == null ? Map.of() : java.util.Collections.unmodifiableMap(content);
    }

    public boolean isSent() {
        return "sent".equals(status);
    }

    public Optional<String> getPdfPath() {
        return Optional.ofNullable(pdfPath);
    }

    public Optional<Instant> getSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
