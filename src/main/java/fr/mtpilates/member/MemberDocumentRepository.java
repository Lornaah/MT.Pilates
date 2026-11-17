package fr.mtpilates.member;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDocumentRepository extends JpaRepository<MemberDocument, UUID> {

    List<MemberDocument> findByUserIdOrderByUploadedAtDesc(UUID userId);
}
