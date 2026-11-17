package fr.mtpilates.calendar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues du calendrier. */
public final class CalendarViews {

    private CalendarViews() {
    }

    public record EventView(UUID id, String start, String end, String startLabel, EventType type, boolean privateEvent,
                            Optional<UUID> coachId, Optional<String> coachName, String coachBadge, String title,
                            Optional<String> description, Optional<String> location, boolean visio) {

        /** « HH:mm » de début (start est un LocalDateTime ISO, heure de Paris). */
        public String startTime() {
            return start.substring(11, 16);
        }

        public String endTime() {
            return end.substring(11, 16);
        }
    }

    /** Événements d'un même jour, sous l'en-tête « lundi 3 mars ». */
    public record DayGroup(String header, List<EventView> events) {
    }

    public record MonthCell(String iso, String day, boolean inMonth, boolean today, List<EventView> events, int more) {
    }

    public record Month(String key, String title, String previous, String next, List<MonthCell> cells) {
    }

    public record CoachChip(UUID id, String name, String badge) {
    }

    public record MemberOption(UUID id, String label) {
    }
}
