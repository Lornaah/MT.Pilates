package fr.mtpilates.announcement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByPublishAtDesc();

    List<Announcement> findByPublishAtLessThanEqualOrderByPublishAtDesc(Instant now);
}
