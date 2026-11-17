package fr.mtpilates.habit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HabitRepository extends JpaRepository<Habit, UUID> {

    List<Habit> findAllByOrderByCreatedAtDesc();

    /** Habitudes d'un membre : les globales et celles qui lui sont assignées, par titre. */
    @Query("select h from Habit h where h.global = true or h.id in ?1 order by h.title")
    List<Habit> findGlobalOrIn(Collection<UUID> ids);
}
