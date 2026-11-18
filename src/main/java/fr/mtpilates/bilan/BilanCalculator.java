package fr.mtpilates.bilan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port de lib/bilan/computeBilan.ts : premières et dernières mesures de la période, écarts, poids de départ (saisi ou
 * première pesée), progression vers l'objectif bornée à [0, 1]. Ajoute les séries hebdomadaires des graphiques.
 */
public final class BilanCalculator {

    private static final int DAYS_PER_WEEK = 7;

    /** Une saisie de suivi : date et valeurs relevées. */
    public record Entry(LocalDate date, Map<BilanMetric, BigDecimal> values) {
    }

    public record Point(LocalDate date, double value) {
    }

    /** Point de graphique : « Début », « Semaine 1 »… avec la dernière valeur connue à cette date. */
    public record WeekPoint(String label, double value) {
    }

    public record Metric(BilanMetric metric, Optional<Double> start, Optional<Double> end, Optional<Double> delta,
                         List<Point> series, List<WeekPoint> weekly) {

        /** Assez de données pour une tuile et un graphique (renderableMetrics de l'original). */
        public boolean renderable() {
            return delta.isPresent() && series.size() >= 2;
        }
    }

    public record Snapshot(LocalDate periodStart, LocalDate periodEnd, List<Metric> metrics, Optional<Double> startWeight,
                           Optional<Double> currentWeight, Optional<Double> targetWeight, Optional<Double> progressRatio,
                           Optional<Double> weightToGo) {

        public Optional<Metric> metric(final BilanMetric key) {
            return metrics.stream().filter(m -> m.metric() == key).findFirst();
        }

        /** Nombre de semaines de la période, arrondi, au moins 1. */
        public int weeks() {
            return (int) Math.max(1, Math.round(ChronoUnit.DAYS.between(periodStart, periodEnd) / (double) DAYS_PER_WEEK));
        }
    }

    private BilanCalculator() {
    }

    public static Snapshot compute(final List<Entry> entries, final LocalDate periodStart, final LocalDate periodEnd,
                                   final Optional<Double> startWeightOverride, final Optional<Double> targetWeight) {
        final List<Entry> inPeriod = entries.stream()
                .filter(e -> !e.date().isBefore(periodStart) && !e.date().isAfter(periodEnd))
                .sorted(Comparator.comparing(Entry::date)).toList();
        final List<Metric> metrics = new ArrayList<>();
        for (final BilanMetric key : BilanMetric.values()) {
            final List<Point> points = inPeriod.stream().filter(e -> e.values().get(key) != null)
                    .map(e -> new Point(e.date(), e.values().get(key).doubleValue())).toList();
            final Optional<Double> start = points.isEmpty() ? Optional.empty() : Optional.of(points.get(0).value());
            final Optional<Double> end = points.isEmpty() ? Optional.empty() : Optional.of(points.get(points.size() - 1).value());
            final Optional<Double> delta = start.flatMap(s -> end.map(e -> round1(e - s)));
            metrics.add(new Metric(key, start, end, delta, points, weekly(points, periodStart, periodEnd)));
        }
        final Metric weight = metrics.get(0);
        final Optional<Double> startWeight = startWeightOverride.or(weight::start);
        final Optional<Double> current = weight.end();
        Optional<Double> ratio = Optional.empty();
        Optional<Double> toGo = Optional.empty();
        if (startWeight.isPresent() && targetWeight.isPresent() && current.isPresent()) {
            final double total = startWeight.get() - targetWeight.get();
            final double progressed = startWeight.get() - current.get();
            // Bornée à [0, 1] : une stagnation ou une reprise ne doit pas donner une barre négative.
            ratio = Optional.of(total > 0 ? Math.max(0, Math.min(1, progressed / total)) : 0);
            toGo = Optional.of(Math.max(0, round1(current.get() - targetWeight.get())));
        }
        return new Snapshot(periodStart, periodEnd, metrics, startWeight, current, targetWeight, ratio, toGo);
    }

    /** « Début » puis une valeur par semaine écoulée : la dernière mesure connue à la fin de chaque semaine. */
    static List<WeekPoint> weekly(final List<Point> points, final LocalDate periodStart, final LocalDate periodEnd) {
        if (points.size() < 2) {
            return List.of();
        }
        final int weeks = (int) Math.max(1, Math.ceil(ChronoUnit.DAYS.between(periodStart, periodEnd) / (double) DAYS_PER_WEEK));
        final List<WeekPoint> series = new ArrayList<>();
        series.add(new WeekPoint("Début", points.get(0).value()));
        for (int w = 1; w <= weeks; w++) {
            final LocalDate limit = w == weeks ? periodEnd : periodStart.plusDays((long) w * DAYS_PER_WEEK);
            final double value = points.stream().filter(p -> !p.date().isAfter(limit)).reduce((a, b) -> b)
                    .orElse(points.get(0)).value();
            series.add(new WeekPoint("Semaine " + w, value));
        }
        return series;
    }

    static double round1(final double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
