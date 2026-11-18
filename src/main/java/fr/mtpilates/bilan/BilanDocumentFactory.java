package fr.mtpilates.bilan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Assemble le {@link BilanDocument} à partir des textes, des variables, des chiffres et des photos choisies. */
public final class BilanDocumentFactory {

    /** Ordre des pages photo, celui du PDF de référence (dos, profil), puis la face. */
    static final List<String> VIEW_ORDER = List.of("back", "side", "front");
    static final Map<String, String> VIEW_SLUGS = Map.of("back", "dos", "side", "profil", "front", "face");
    private static final int WEIGHT_CHART_W = 820;
    private static final int WEIGHT_CHART_H = 360;
    private static final int MEASURE_CHART_W = 600;
    private static final int MEASURE_CHART_H = 300;
    private static final double PERCENT = 100;

    /** Photo chargée (octets) puis annotée ; vide si la photo n'existe plus. */
    public interface PhotoSource {
        Optional<byte[]> bytes(String photoId);
    }

    private BilanDocumentFactory() {
    }

    public static BilanDocument build(final BilanContent content, final BilanCalculator.Snapshot snapshot,
                                      final Map<String, String> variables, final PhotoSource photos) {
        final Map<String, String> texts = content.textsOrEmpty();
        final Map<String, String> html = new LinkedHashMap<>();
        final Map<String, List<BilanDocument.Point>> points = new LinkedHashMap<>();
        texts.forEach((key, text) -> {
            html.put(key, BilanMarkup.html(text, variables));
            if (key.endsWith(".points")) {
                points.put(key, BilanMarkup.points(BilanMarkup.resolve(text, variables)).stream()
                        .map(p -> new BilanDocument.Point(BilanMarkup.html(p.title(), Map.of()), BilanMarkup.html(p.text(), Map.of())))
                        .toList());
            }
        });
        final List<BilanMetric> selected = selected(content, snapshot);
        final List<BilanDocument.ViewPage> views = views(content, photos);
        return new BilanDocument(html, points, tiles(selected, snapshot, html), views, weightChart(selected, snapshot),
                measures(selected, snapshot), objective(snapshot), views.stream().reduce((a, b) -> b).map(BilanDocument.ViewPage::after));
    }

    /** Mensurations cochées ayant assez de mesures, dans l'ordre du calcul. */
    static List<BilanMetric> selected(final BilanContent content, final BilanCalculator.Snapshot snapshot) {
        return snapshot.metrics().stream().filter(BilanCalculator.Metric::renderable)
                .map(BilanCalculator.Metric::metric).filter(m -> content.measuresOrEmpty().contains(m.key())).toList();
    }

    private static List<BilanDocument.Tile> tiles(final List<BilanMetric> selected, final BilanCalculator.Snapshot s,
                                                  final Map<String, String> html) {
        return selected.stream().map(m -> {
            final double delta = s.metric(m).flatMap(BilanCalculator.Metric::delta).orElse(0.0);
            final String label = m == BilanMetric.WEIGHT && delta < 0 ? "Poids perdu" : m.label();
            return new BilanDocument.Tile(BilanVariables.delta(delta, m.unit()), label, html.getOrDefault("chiffres." + m.slug(), ""));
        }).toList();
    }

    private static Optional<String> weightChart(final List<BilanMetric> selected, final BilanCalculator.Snapshot s) {
        if (!selected.contains(BilanMetric.WEIGHT)) {
            return Optional.empty();
        }
        return s.metric(BilanMetric.WEIGHT).map(m -> BilanCharts.area(m.weekly(), "Poids (kg)", BilanCharts.Style.WEIGHT,
                s.targetWeight(), WEIGHT_CHART_W, WEIGHT_CHART_H));
    }

    private static List<BilanDocument.MeasurePage> measures(final List<BilanMetric> selected, final BilanCalculator.Snapshot s) {
        final List<BilanDocument.MeasurePage> pages = new ArrayList<>();
        selected.stream().filter(m -> m != BilanMetric.WEIGHT).forEach(m -> s.metric(m).ifPresent(metric -> pages.add(
                new BilanDocument.MeasurePage(m.slug(), BilanVariables.delta(metric.delta().orElse(0.0), m.unit()),
                        BilanCharts.area(metric.weekly(), m.label() + " (" + m.unit() + ")", BilanCharts.Style.MEASURE,
                                Optional.empty(), MEASURE_CHART_W, MEASURE_CHART_H)))));
        return pages;
    }

    private static Optional<BilanDocument.Objective> objective(final BilanCalculator.Snapshot s) {
        return s.progressRatio().map(r -> new BilanDocument.Objective(BilanVariables.number(r * PERCENT) + "%", r * PERCENT));
    }

    /** Vues cochées dont les deux photos existent encore. */
    private static List<BilanDocument.ViewPage> views(final BilanContent content, final PhotoSource photos) {
        final List<BilanDocument.ViewPage> pages = new ArrayList<>();
        for (final String code : VIEW_ORDER) {
            final BilanContent.View view = content.viewsOrEmpty().get(code);
            if (view == null || !view.enabled() || view.before() == null || view.after() == null) {
                continue;
            }
            final Optional<String> before = photos.bytes(view.before())
                    .flatMap(b -> BilanImages.annotatedPhoto(b, view.beforeStrokesOrEmpty()));
            final Optional<String> after = photos.bytes(view.after())
                    .flatMap(b -> BilanImages.annotatedPhoto(b, view.afterStrokesOrEmpty()));
            if (before.isPresent() && after.isPresent()) {
                pages.add(new BilanDocument.ViewPage(VIEW_SLUGS.get(code), before.get(), after.get()));
            }
        }
        return pages;
    }
}
