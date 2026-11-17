package fr.mtpilates.training;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findAllByOrderByCreatedAtDesc();

    /** Programmes d'un membre : les siens et les communs (assignee nul), plus récents d'abord. */
    @Query("select p from Program p where p.assigneeId = ?1 or p.assigneeId is null order by p.createdAt desc")
    List<Program> findForMember(UUID memberId);
}
