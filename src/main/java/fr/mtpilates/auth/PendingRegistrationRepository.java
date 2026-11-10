package fr.mtpilates.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {

    @Query("select p from PendingRegistration p where lower(p.email) = lower(?1)")
    Optional<PendingRegistration> findByEmail(String email);

    Optional<PendingRegistration> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PendingRegistration p where p.expiresAt <= ?1")
    int deleteExpired(Instant now);
}
