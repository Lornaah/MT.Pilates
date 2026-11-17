package fr.mtpilates.tracking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPhotoRepository extends JpaRepository<MemberPhoto, UUID> {

    List<MemberPhoto> findByUserIdOrderByTakenAtDesc(UUID userId);
}
