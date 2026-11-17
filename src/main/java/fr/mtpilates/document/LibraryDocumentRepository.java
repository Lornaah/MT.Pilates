package fr.mtpilates.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryDocumentRepository extends JpaRepository<LibraryDocument, UUID> {

    List<LibraryDocument> findAllByOrderByCreatedAtDesc();
}
