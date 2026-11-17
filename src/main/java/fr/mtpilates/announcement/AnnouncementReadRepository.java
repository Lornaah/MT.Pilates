package fr.mtpilates.announcement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    List<AnnouncementRead> findByUserId(UUID userId);

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, UUID userId);
}
