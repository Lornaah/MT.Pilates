package fr.mtpilates.bilan;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberReportRepository extends JpaRepository<MemberReport, UUID> {

    List<MemberReport> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<MemberReport> findByUserIdAndStatusOrderBySentAtDesc(UUID userId, String status);

    java.util.Optional<MemberReport> findFirstByUserIdAndTargetWeightKgNotNullOrderByCreatedAtDesc(UUID userId);
}
