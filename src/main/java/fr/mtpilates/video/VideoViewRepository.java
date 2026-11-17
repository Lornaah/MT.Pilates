package fr.mtpilates.video;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoViewRepository extends JpaRepository<VideoView, UUID> {

    long countByUserIdAndWatchedAtGreaterThanEqual(UUID userId, Instant since);

    @Query("select v.watchedAt from VideoView v where v.userId = ?1 order by v.watchedAt desc")
    List<Instant> findRecentWatchedAt(UUID userId, Limit limit);

    @EntityGraph(attributePaths = "video")
    List<VideoView> findByUserIdOrderByWatchedAtDesc(UUID userId, Limit limit);
}
