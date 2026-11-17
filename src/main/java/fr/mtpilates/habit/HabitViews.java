package fr.mtpilates.habit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vues des habitudes (bibliothèque coach, carte du membre, suivi sur la fiche). */
public final class HabitViews {

    private HabitViews() {
    }

    public record LibraryItem(UUID id, String title, Optional<String> description, String icon, String iconName,
                              String frequency, String frequencyLabel, String frequencyVariant, boolean global) {
    }

    public record TodayItem(UUID id, String title, Optional<String> description, String icon, boolean checked,
                            int streak) {
    }

    public record Assignee(UUID id, String name, String email, boolean selected) {
    }

    public record TrackedDay(String title, boolean done) {
    }

    public record TrackedHabit(String title, String icon, int streak, List<TrackedDay> days, int done, int rate) {
    }

    public record Tracking(int range, int habitCount, int globalRate, String fromLabel, List<TrackedHabit> habits) {
    }
}
