package fr.mtpilates.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramDayCompletionRepository extends JpaRepository<ProgramDayCompletion, UUID> {

    List<ProgramDayCompletion> findByUserId(UUID userId);

    Optional<ProgramDayCompletion> findByProgramDayIdAndUserId(UUID programDayId, UUID userId);
}
