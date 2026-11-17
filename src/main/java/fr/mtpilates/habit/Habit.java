package fr.mtpilates.habit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Habitude proposée aux membres, globale ou assignée (table habits). */
@Entity
@Table(name = "habits")
public class Habit {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    private String icon;

    @Column(nullable = false)
    private String frequency = "daily";

    @Column(name = "is_global", nullable = false)
    private boolean global;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Habit() {
    }

    public Habit(final UUID createdBy) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
    }

    public void update(final String newTitle, final String newDescription, final String newIcon,
                       final String newFrequency, final boolean isGlobal) {
        this.title = newTitle;
        this.description = newDescription;
        this.icon = newIcon;
        this.frequency = newFrequency;
        this.global = isGlobal;
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

    public Optional<String> getIcon() {
        return Optional.ofNullable(icon);
    }

    public String getFrequency() {
        return frequency;
    }

    public boolean isGlobal() {
        return global;
    }
}
