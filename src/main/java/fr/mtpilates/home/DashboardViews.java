package fr.mtpilates.home;

import fr.mtpilates.calendar.CalendarViews.EventView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues des tableaux de bord. */
public final class DashboardViews {

    private DashboardViews() {
    }

    public record Stats(long active, long paused, long archived, long total) {
    }

    public record Upcoming(UUID id, String title, String month, String day, String when, Optional<UUID> memberId) {
    }

    public record Birthday(String name, String dateLabel, Optional<String> whatsapp) {
    }

    public record Staff(String firstName, Stats stats, String todayLabel, List<EventView> today, List<Upcoming> oneOnOnes,
                        List<Upcoming> adminTasks, List<Birthday> birthdays) {
    }

    public record WeekEvent(String time, String title) {
    }

    public record WeekDay(String weekday, String day, boolean today, List<WeekEvent> events) {
    }

    public record Coach(String firstName, String fullName, Optional<String> avatarUrl, String initial) {
    }

    public record Member(Optional<String> firstName, Optional<Coach> coach, long streakDays, long doneThisMonth,
                         int monthlyTarget, String weekLabel, List<WeekDay> week, boolean hasNutrition) {
    }
}
