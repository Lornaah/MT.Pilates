package fr.mtpilates.tracking;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.tracking.TrackingViews.PhotoView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Données du graphique d'évolution : points bruts, export CSV et comparaison de deux dates (TrackingChartCard). */
@Service
public class TrackingChartService {

    /** Mesures tracées, dans l'ordre de l'original (la masse grasse n'y figure pas). */
    public static final List<Measure> CHARTED = List.of(Measure.WEIGHT, Measure.WAIST, Measure.HIP, Measure.CHEST,
            Measure.ARM, Measure.THIGH);

    private static final List<String> CSV_COLUMNS = List.of("date", "weight_kg", "body_fat_pct", "waist_cm", "hip_cm",
            "chest_cm", "arm_cm", "thigh_cm", "notes");
    private static final int PHOTOS_PER_DATE = 3;

    private final TrackingService tracking;
    private final MemberPhotoService photos;

    public TrackingChartService(final TrackingService tracking, final MemberPhotoService photos) {
        this.tracking = tracking;
        this.photos = photos;
    }

    /** Points du graphique, du plus ancien au plus récent : date ISO, libellé « d MMM » et valeurs brutes. */
    public List<Map<String, Object>> points(final CurrentUser user, final UUID memberId) {
        return tracking.history(user, memberId).stream().map(TrackingChartService::point).toList();
    }

    /** Export CSV de l'original : colonnes fixes, plus récent en premier, guillemets doublés si besoin. */
    public String csv(final CurrentUser user, final UUID memberId) {
        final List<TrackingEntry> newestFirst = tracking.history(user, memberId).reversed();
        final StringBuilder csv = new StringBuilder(String.join(",", CSV_COLUMNS)).append('\n');
        csv.append(newestFirst.stream().map(TrackingChartService::csvRow).collect(Collectors.joining("\n")));
        return csv.toString();
    }

    public Comparison compare(final CurrentUser user, final UUID memberId, final Optional<LocalDate> a,
                              final Optional<LocalDate> b) {
        final List<TrackingEntry> entries = tracking.history(user, memberId);
        final List<PhotoView> memberPhotos = photos.photos(user, memberId);
        final Optional<Snapshot> left = a.map(date -> snapshot(entries, memberPhotos, date));
        final Optional<Snapshot> right = b.map(date -> snapshot(entries, memberPhotos, date));
        return new Comparison(left, right, deltas(left, right));
    }

    /** Colonne d'une date : mesure la plus proche (si une existe) et photos les plus proches. */
    public record Snapshot(boolean hasEntry, String dateLabel, List<Line> lines, List<PhotoView> photos,
                           Map<Measure, BigDecimal> raw) {
    }

    public record Line(String label, String value) {
    }

    public record Delta(String label, String from, String to, String change, int sign) {
    }

    public record Comparison(Optional<Snapshot> left, Optional<Snapshot> right, List<Delta> deltas) {

        public boolean any() {
            return left.map(Snapshot::hasEntry).orElse(false) || right.map(Snapshot::hasEntry).orElse(false);
        }
    }

    private static Map<String, Object> point(final TrackingEntry entry) {
        final Map<String, Object> point = new LinkedHashMap<>();
        point.put("date", entry.getDate().toString());
        point.put("label", FrenchDates.shortDate(entry.getDate()));
        final Map<Measure, BigDecimal> values = entry.values();
        CHARTED.forEach(m -> point.put(m.param(), Optional.ofNullable(values.get(m)).map(BigDecimal::doubleValue).orElse(null)));
        return point;
    }

    private static String csvRow(final TrackingEntry entry) {
        final Map<Measure, BigDecimal> values = entry.values();
        final List<String> cells = new ArrayList<>();
        cells.add(entry.getDate().toString());
        for (final Measure measure : List.of(Measure.WEIGHT, Measure.BODY_FAT, Measure.WAIST, Measure.HIP, Measure.CHEST,
                Measure.ARM, Measure.THIGH)) {
            cells.add(Optional.ofNullable(values.get(measure)).map(TrackingService::plain).orElse(""));
        }
        cells.add(entry.getNotes().orElse(""));
        return cells.stream().map(TrackingChartService::csvCell).collect(Collectors.joining(","));
    }

    private static String csvCell(final String value) {
        final String escaped = value.replace("\"", "\"\"");
        return escaped.matches("(?s).*[\",\n].*") ? "\"" + escaped + "\"" : escaped;
    }

    /** Entrée la plus proche de la date (la première en cas d'égalité) et les trois photos les plus proches. */
    private static Snapshot snapshot(final List<TrackingEntry> entries, final List<PhotoView> memberPhotos,
                                     final LocalDate target) {
        final Optional<TrackingEntry> closest = entries.reversed().stream()
                .min(Comparator.comparingLong(e -> distance(e.getDate(), target)));
        final List<PhotoView> nearestPhotos = memberPhotos.stream()
                .sorted(Comparator.comparingLong(p -> distance(LocalDate.parse(p.takenAtIso()), target)))
                .limit(PHOTOS_PER_DATE).toList();
        final Map<Measure, BigDecimal> raw = closest.map(TrackingEntry::values).orElse(Map.of());
        return new Snapshot(closest.isPresent(), closest.map(e -> FrenchDates.medium(e.getDate())).orElse("—"),
                CHARTED.stream().filter(raw::containsKey)
                        .map(m -> new Line(chartLabel(m), TrackingService.plain(raw.get(m)))).toList(),
                nearestPhotos, raw);
    }

    private static List<Delta> deltas(final Optional<Snapshot> left, final Optional<Snapshot> right) {
        if (!left.map(Snapshot::hasEntry).orElse(false) || !right.map(Snapshot::hasEntry).orElse(false)) {
            return List.of();
        }
        return CHARTED.stream()
                .filter(m -> left.get().raw().containsKey(m) && right.get().raw().containsKey(m))
                .map(m -> {
                    final BigDecimal from = left.get().raw().get(m);
                    final BigDecimal to = right.get().raw().get(m);
                    final BigDecimal change = to.subtract(from);
                    return new Delta(chartLabel(m), TrackingService.plain(from), TrackingService.plain(to),
                            (change.signum() > 0 ? "+" : "") + change.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                            change.signum());
                })
                .toList();
    }

    private static long distance(final LocalDate a, final LocalDate b) {
        return Math.abs(ChronoUnit.DAYS.between(a, b));
    }

    /** Libellés courts du graphique de l'original. */
    public static String chartLabel(final Measure measure) {
        return switch (measure) {
            case WEIGHT -> "Poids (kg)";
            case WAIST -> "Tour de taille";
            case HIP -> "Hanches";
            case CHEST -> "Poitrine";
            case ARM -> "Bras";
            case THIGH -> "Cuisses";
            case BODY_FAT -> "Masse grasse (%)";
        };
    }
}
