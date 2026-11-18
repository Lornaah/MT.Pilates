package fr.mtpilates.bilan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Valeurs des variables du modèle de textes, au format français (« 63,7 kg », « 27,5% »). */
public final class BilanVariables {

    private static final double DAYS_PER_MONTH = 30.44;
    private static final String MISSING = "—";

    private BilanVariables() {
    }

    /** Durées calculées : mois de suivi (titre), durée de la période en mois, nombre de semaines. */
    public static Map<String, String> computedDurations(final BilanCalculator.Snapshot s, final Optional<LocalDate> followStart) {
        final long periodDays = ChronoUnit.DAYS.between(s.periodStart(), s.periodEnd());
        final long sinceStart = ChronoUnit.DAYS.between(followStart.filter(d -> d.isBefore(s.periodStart())).orElse(s.periodStart()),
                s.periodEnd());
        final int monthIndex = (int) Math.max(1, Math.round(sinceStart / DAYS_PER_MONTH));
        final int months = (int) Math.max(1, Math.round(periodDays / DAYS_PER_MONTH));
        final int weeks = s.weeks();
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("duree_titre", (monthIndex == 1 ? "1er" : monthIndex + "e") + " MOIS");
        map.put("duree", months + " mois");
        map.put("semaines", weeks + (weeks > 1 ? " semaines" : " semaine"));
        return map;
    }

    /** Toutes les variables : durées (calculées, ou remplacées par le coach) et chiffres du bilan. */
    public static Map<String, String> of(final String firstName, final BilanCalculator.Snapshot s,
                                         final Map<String, String> durations) {
        final Map<String, String> map = new LinkedHashMap<>(durations);
        map.put("prenom", firstName);
        map.put("poids_depart", kg(s.startWeight()));
        map.put("poids_actuel", kg(s.currentWeight()));
        map.put("poids_objectif", kg(s.targetWeight()));
        map.put("poids_perdu", kg(s.startWeight().flatMap(a -> s.currentWeight().map(b -> Math.max(0, a - b)))));
        map.put("kg_a_perdre", kg(s.startWeight().flatMap(a -> s.targetWeight().map(b -> Math.max(0, a - b)))));
        map.put("kg_restants", kg(s.weightToGo()));
        map.put("progression", s.progressRatio().map(r -> number(r * 100) + "%").orElse(MISSING));
        return map;
    }

    /** Écart signé d'une mesure (« -3,3 kg », « +1 cm »). */
    public static String delta(final double value, final String unit) {
        return (value > 0 ? "+" : "") + number(value) + " " + unit;
    }

    private static String kg(final Optional<Double> value) {
        return value.map(v -> number(v) + " kg").orElse(MISSING);
    }

    /** Une décimale au plus, virgule française, sans « ,0 ». */
    public static String number(final double value) {
        final BigDecimal rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return (rounded.scale() < 0 ? rounded.setScale(0) : rounded).toPlainString().replace('.', ',')
                .toLowerCase(Locale.ROOT);
    }
}
