package fr.mtpilates.prospect;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectRepository extends JpaRepository<Prospect, UUID> {

    List<Prospect> findAllByOrderByCreatedAtDesc();
}
