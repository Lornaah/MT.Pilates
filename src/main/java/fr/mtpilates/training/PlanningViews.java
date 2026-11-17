package fr.mtpilates.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues du planning membre (MyPlanning) et de la carte « Programmes du membre ». */
public final class PlanningViews {

    private PlanningViews() {
    }

    public record SessionLink(UUID id, String title) {
    }

    public record DayView(UUID id, String dayLabel, String typeLabel, String icon, String title,
                          Optional<String> comment, boolean workout, List<SessionLink> sessions, boolean completed) {
    }

    /** Les 7 cases d'une semaine ; une case vide est un jour sans entrée en base. */
    public record WeekView(int number, Optional<String> comment, List<Optional<DayView>> days) {

        public String dayLabel(final int index) {
            return DAY_LABELS.get(index);
        }
    }

    public record Planning(String title, Optional<String> description, List<WeekView> weeks, int currentWeek,
                           Optional<DayView> today, String todayLabel) {

        public Optional<WeekView> current() {
            return weeks.isEmpty() ? Optional.empty() : Optional.of(weeks.get(currentWeek));
        }
    }

    public record MemberProgram(UUID id, String title, String statusLabel, Optional<String> startLabel,
                                Optional<Integer> durationWeeks) {
    }

    public record MemberProgramSection(String title, List<MemberProgram> programs) {
    }

    static final List<String> DAY_LABELS = List.of("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim");
}
