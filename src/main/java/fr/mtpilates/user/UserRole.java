package fr.mtpilates.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private AppRole role;

    protected UserRole() {
    }

    public UserRole(final UUID userId, final AppRole role) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public AppRole getRole() {
        return role;
    }
}
