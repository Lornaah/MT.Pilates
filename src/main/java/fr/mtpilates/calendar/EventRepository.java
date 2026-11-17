package fr.mtpilates.calendar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findAllByOrderByStartsAtAsc();

    /** Événements d'un membre du staff : ceux qu'il anime ou qu'il a créés. */
    @Query("select e from Event e where e.coachId = ?1 or e.createdBy = ?1 order by e.startsAt")
    List<Event> findForStaff(UUID userId);

    @Query("select e from Event e where (e.coachId = ?1 or e.createdBy = ?1) and e.startsAt >= ?2 and e.startsAt <= ?3 order by e.startsAt")
    List<Event> findForStaffBetween(UUID userId, Instant from, Instant to);

    @Query("select e from Event e where (e.coachId = ?1 or e.createdBy = ?1) and e.type = ?2 and e.startsAt >= ?3 order by e.startsAt")
    List<Event> findUpcomingOfType(UUID userId, String type, Instant from, Limit limit);

    @Query("select e from Event e where e.startsAt >= ?1 and e.startsAt < ?2 order by e.startsAt")
    List<Event> findBetween(Instant from, Instant to);
}
