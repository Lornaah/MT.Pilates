package fr.mtpilates.habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    List<HabitCompletion> findByUserIdAndCompletedDateBetween(UUID userId, LocalDate from, LocalDate to);

    Optional<HabitCompletion> findByHabitIdAndUserIdAndCompletedDate(UUID habitId, UUID userId, LocalDate date);
}
