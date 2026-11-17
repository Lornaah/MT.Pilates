package fr.mtpilates.nutrition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, UUID> {

    List<NutritionPlan> findByActiveTrueOrderByUpdatedAtDesc();

    Optional<NutritionPlan> findFirstByMemberIdAndActiveTrueOrderByCreatedAtDesc(UUID memberId);
}
