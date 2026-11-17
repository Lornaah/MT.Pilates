package fr.mtpilates.video;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByPublishedTrueOrderByCreatedAtDesc();

    List<Video> findAllByOrderByCreatedAtDesc();
}
