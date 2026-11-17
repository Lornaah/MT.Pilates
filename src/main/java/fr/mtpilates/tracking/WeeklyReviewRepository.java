package fr.mtpilates.tracking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReviewRepository extends JpaRepository<WeeklyReview, UUID> {

    List<WeeklyReview> findByUserIdOrderByWeekStartDateDesc(UUID userId);

    Optional<WeeklyReview> findByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);
}
