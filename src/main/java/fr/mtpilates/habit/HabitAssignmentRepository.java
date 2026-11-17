package fr.mtpilates.habit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitAssignmentRepository extends JpaRepository<HabitAssignment, UUID> {

    List<HabitAssignment> findByHabitId(UUID habitId);

    List<HabitAssignment> findByUserId(UUID userId);

    void deleteByHabitIdAndUserId(UUID habitId, UUID userId);
}
