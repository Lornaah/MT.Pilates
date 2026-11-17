package fr.mtpilates.video;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoViewRepository extends JpaRepository<VideoView, UUID> {

    @EntityGraph(attributePaths = "video")
    List<VideoView> findByUserIdOrderByWatchedAtDesc(UUID userId, Limit limit);
}
