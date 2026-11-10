package fr.mtpilates.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    @Query("select u from UserAccount u where lower(u.email) = lower(?1)")
    Optional<UserAccount> findByEmail(String email);

    @Query("select count(u) > 0 from UserAccount u where lower(u.email) = lower(?1)")
    boolean existsByEmail(String email);
}
