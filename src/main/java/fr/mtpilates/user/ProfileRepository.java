package fr.mtpilates.user;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findAllByOrderByCreatedAtDesc();

    List<Profile> findByCoachIdOrderByCreatedAtDesc(UUID coachId);

    /** Fiches visibles d'un non-manager : la sienne et celles des membres qu'il coache. */
    @Query("select p from Profile p where p.id = ?1 or p.coachId = ?1 order by p.createdAt desc")
    List<Profile> findOwnOrCoachedOrderByCreatedAtDesc(UUID userId);

    List<Profile> findByIdInOrderByCreatedAtAsc(Collection<UUID> ids);
}
