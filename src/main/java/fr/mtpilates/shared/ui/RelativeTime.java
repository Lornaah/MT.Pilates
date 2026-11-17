package fr.mtpilates.shared.ui;

import java.time.Duration;
import java.time.Instant;

/** formatDistanceToNow(…, { addSuffix: true, locale: fr }) de date-fns : « il y a 3 jours », « dans environ 2 heures ». */
public final class RelativeTime {

    private static final long MINUTE = 60;
    private static final long HOUR = 3600;
    private static final long DAY = 86_400;
    private static final long MONTH = DAY * 30;
    private static final long YEAR = DAY * 365;

    private RelativeTime() {
    }

    public static String between(final Instant date, final Instant now) {
        final long seconds = Duration.between(date, now).getSeconds();
        final String distance = distance(Math.abs(seconds));
        return seconds >= 0 ? "il y a " + distance : "dans " + distance;
    }

    static String distance(final long s) {
        final long minutes = Math.round(s / (double) MINUTE);
        if (s < 30) {
            return "moins d’une minute";
        }
        if (minutes < 2) {
            return "1 minute";
        }
        if (minutes < 45) {
            return minutes + " minutes";
        }
        if (minutes < 90) {
            return "environ 1 heure";
        }
        if (s < DAY) {
            return "environ " + Math.round(s / (double) HOUR) + " heures";
        }
        if (s < 42 * HOUR) {
            return "1 jour";
        }
        if (s < MONTH) {
            return Math.round(s / (double) DAY) + " jours";
        }
        if (s < 45 * DAY) {
            return "environ 1 mois";
        }
        if (s < 60 * DAY) {
            return "environ 2 mois";
        }
        if (s < YEAR) {
            return Math.round(s / (double) MONTH) + " mois";
        }
        final long years = s / YEAR;
        final long remainder = (s % YEAR) / MONTH;
        if (remainder < 3) {
            return years == 1 ? "environ 1 an" : "environ " + years + " ans";
        }
        if (remainder < 9) {
            return years == 1 ? "plus d’un an" : "plus de " + years + " ans";
        }
        return "presque " + (years + 1) + " ans";
    }
}
