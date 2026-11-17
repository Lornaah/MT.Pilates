package fr.mtpilates.prospect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Lead du pipeline commercial (table prospects). */
@Entity
@Table(name = "prospects")
public class Prospect {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    private String phone;

    private Integer age;

    private String source;

    @Column(nullable = false)
    private String status = "new";

    private String tag;

    private String notes;

    private String objectives;

    @Column(name = "next_followup_at")
    private Instant nextFollowupAt;

    @Column(name = "appointment_at")
    private Instant appointmentAt;

    @Column(name = "converted_member_id")
    private UUID convertedMemberId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Prospect() {
    }

    public Prospect(final UUID createdBy, final Instant now) {
        this.id = UUID.randomUUID();
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(final ProspectContent c, final Instant now) {
        this.firstName = c.firstName();
        this.lastName = c.lastName();
        this.email = c.email();
        this.phone = c.phone();
        this.age = c.age();
        this.source = c.source();
        this.status = c.status();
        this.tag = c.tag();
        this.notes = c.notes();
        this.objectives = c.objectives();
        this.nextFollowupAt = c.nextFollowupAt();
        this.appointmentAt = c.appointmentAt();
        this.updatedAt = now;
    }

    public void changeStatus(final String newStatus, final Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void convertedTo(final UUID memberId, final Instant now) {
        this.status = "converted";
        this.convertedMemberId = memberId;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public ProspectContent content() {
        return new ProspectContent(firstName, lastName, email, phone, age, source, status, tag, notes, objectives,
                nextFollowupAt, appointmentAt);
    }

    public Optional<UUID> getConvertedMemberId() {
        return Optional.ofNullable(convertedMemberId);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
