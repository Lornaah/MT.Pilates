package fr.mtpilates.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Inscription en attente de confirmation par email (le compte n'existe pas encore). */
@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected PendingRegistration() {
    }

    public PendingRegistration(final String email, final String passwordHash, final String tokenHash, final Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void renew(final String newTokenHash, final Instant newExpiresAt) {
        this.tokenHash = newTokenHash;
        this.expiresAt = newExpiresAt;
    }

    public void replacePassword(final String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public boolean isExpired(final Instant now) {
        return !expiresAt.isAfter(now);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
