package fr.mtpilates.tracking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEntryRepository extends JpaRepository<TrackingEntry, UUID> {

    List<TrackingEntry> findByUserIdOrderByDateDesc(UUID userId, Limit limit);
}
