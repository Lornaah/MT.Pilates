package fr.mtpilates.shared.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats de date de l'original (date-fns, locale fr). */
public final class FrenchDates {

    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FR);
    private static final DateTimeFormatter MEDIUM = DateTimeFormatter.ofPattern("d MMM yyyy", FR);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", FR);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", FR);
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("d MMM HH:mm", FR);
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("EEEE d MMM, HH:mm", FR);

    private FrenchDates() {
    }

    /** « lundi 3 mars 2026 ». */
    public static String full(final LocalDate date) {
        return date.format(FULL);
    }

    /** « 3 mars 2026 », « 12 janv. 2026 ». */
    public static String medium(final LocalDate date) {
        return date.format(MEDIUM);
    }

    /** « 3 mars ». */
    public static String dayMonth(final LocalDate date) {
        return date.format(DAY_MONTH);
    }

    /** « 3 mars », « 12 janv. ». */
    public static String shortDate(final LocalDate date) {
        return date.format(SHORT);
    }

    /** « 3 mars 14:30 ». */
    public static String shortDateTime(final LocalDateTime dateTime) {
        return dateTime.format(SHORT_TIME);
    }

    /** « lundi 3 mars, 14:30 ». */
    public static String dayDateTime(final LocalDateTime dateTime) {
        return dateTime.format(DAY_TIME);
    }
}
