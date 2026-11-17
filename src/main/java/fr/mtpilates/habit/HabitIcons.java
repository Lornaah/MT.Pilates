package fr.mtpilates.habit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Icônes autorisées pour une habitude (HABIT_ICONS) : nom stocké → icône Lucide. Inconnue ou absente : sparkles. */
public final class HabitIcons {

    private static final Map<String, String> ICONS = new LinkedHashMap<>();

    static {
        for (final String name : List.of("droplets", "sun", "moon", "apple", "salad", "leaf", "coffee", "pill", "dumbbell",
                "footprints", "bed", "brain", "heart", "smile", "sparkles", "target", "timer", "waves", "wind")) {
            ICONS.put(name, name);
        }
        ICONS.put("bookopen", "book-open");
        ICONS.put("activity", "activity");
    }

    private HabitIcons() {
    }

    /** Nom stocké → nom de fichier Lucide, dans l'ordre du sélecteur. */
    public static Map<String, String> all() {
        return Map.copyOf(ICONS);
    }

    public static List<String> names() {
        return List.copyOf(ICONS.keySet());
    }

    public static String lucide(final Optional<String> name) {
        return name.map(n -> ICONS.get(n.toLowerCase(Locale.ROOT))).orElse("sparkles");
    }

    public static String lucide(final String name) {
        return lucide(Optional.ofNullable(name));
    }

    public static boolean isKnown(final String name) {
        return name != null && ICONS.containsKey(name.toLowerCase(Locale.ROOT));
    }
}
