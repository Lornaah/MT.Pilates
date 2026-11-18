package fr.mtpilates.bilan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BilanCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    private static BilanCalculator.Entry entry(final int day, final String weight, final String hip) {
        final Map<BilanMetric, BigDecimal> values = new java.util.EnumMap<>(BilanMetric.class);
        if (weight != null) {
            values.put(BilanMetric.WEIGHT, new BigDecimal(weight));
        }
        if (hip != null) {
            values.put(BilanMetric.HIP, new BigDecimal(hip));
        }
        return new BilanCalculator.Entry(START.plusDays(day), values);
    }

    private final List<BilanCalculator.Entry> entries = List.of(entry(-3, "70", null), entry(0, "67", "97"),
            entry(8, "66", null), entry(15, "65.2", "95"), entry(28, "63.7", "93"), entry(40, "60", "90"));

    @Test
    void computesDeltasAndProgressLikeTheOriginal() {
        final BilanCalculator.Snapshot s = BilanCalculator.compute(entries, START, START.plusDays(28), Optional.empty(),
                Optional.of(55.0));

        assertThat(s.metric(BilanMetric.WEIGHT).orElseThrow().delta()).contains(-3.3);
        assertThat(s.metric(BilanMetric.HIP).orElseThrow().delta()).contains(-4.0);
        assertThat(s.metric(BilanMetric.ARM).orElseThrow().renderable()).isFalse();
        assertThat(s.startWeight()).contains(67.0);
        assertThat(s.progressRatio().orElseThrow()).isCloseTo(0.275, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(s.weightToGo()).contains(8.7);
        assertThat(s.weeks()).isEqualTo(4);
    }

    @Test
    void weeklySeriesCarriesTheLastKnownValue() {
        final BilanCalculator.Snapshot s = BilanCalculator.compute(entries, START, START.plusDays(28), Optional.empty(),
                Optional.empty());

        assertThat(s.metric(BilanMetric.WEIGHT).orElseThrow().weekly()).extracting(BilanCalculator.WeekPoint::label,
                        BilanCalculator.WeekPoint::value)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Début", 67.0), org.assertj.core.groups.Tuple.tuple("Semaine 1", 67.0),
                        org.assertj.core.groups.Tuple.tuple("Semaine 2", 66.0), org.assertj.core.groups.Tuple.tuple("Semaine 3", 65.2),
                        org.assertj.core.groups.Tuple.tuple("Semaine 4", 63.7));
    }

    @Test
    void overrideAndStallKeepTheProgressBetweenZeroAndOne() {
        final BilanCalculator.Snapshot s = BilanCalculator.compute(entries, START, START.plusDays(28), Optional.of(62.0),
                Optional.of(55.0));

        assertThat(s.progressRatio()).contains(0.0);
    }

    @Test
    void variablesAreFormattedTheFrenchWay() {
        final BilanCalculator.Snapshot s = BilanCalculator.compute(entries, START, START.plusDays(28), Optional.empty(),
                Optional.of(55.0));
        final Map<String, String> durations = BilanVariables.computedDurations(s, Optional.of(START.minusDays(3)));
        final Map<String, String> vars = BilanVariables.of("Rina", s, durations);

        assertThat(durations).containsEntry("duree_titre", "1er MOIS").containsEntry("duree", "1 mois")
                .containsEntry("semaines", "4 semaines");
        assertThat(vars).containsEntry("poids_depart", "67 kg").containsEntry("poids_actuel", "63,7 kg")
                .containsEntry("kg_a_perdre", "12 kg").containsEntry("poids_perdu", "3,3 kg").containsEntry("progression", "27,5%");
        assertThat(BilanVariables.delta(-3.3, "kg")).isEqualTo("-3,3 kg");
        assertThat(BilanVariables.computedDurations(BilanCalculator.compute(entries, START, START.plusDays(28), Optional.empty(),
                Optional.empty()), Optional.of(START.minusDays(62)))).containsEntry("duree_titre", "3e MOIS");
    }

    @Test
    void markupIsEscapedThenFormatted() {
        assertThat(BilanMarkup.html("En **seulement {duree}** <b>, =={prenom}== __ok__\nfin", Map.of("duree", "1 mois", "prenom", "A&B")))
                .isEqualTo("En <strong>seulement 1 mois</strong> &lt;b&gt;, <span class=\"hl\">A&amp;B</span> <u>ok</u><br/>fin");
        assertThat(BilanMarkup.points("Ta régularité : Tu étais là.\n\nSeul")).containsExactly(
                new BilanMarkup.Point("Ta régularité", "Tu étais là."), new BilanMarkup.Point("Seul", ""));
    }

    @Test
    void everyTemplateVariableIsProvided() {
        final BilanCalculator.Snapshot s = BilanCalculator.compute(entries, START, START.plusDays(28), Optional.empty(),
                Optional.of(55.0));
        final Map<String, String> vars = BilanVariables.of("Rina", s, BilanVariables.computedDurations(s, Optional.empty()));

        assertThat(vars.keySet()).containsAll(BilanTemplateCatalog.VARIABLES.keySet());
        BilanTemplateCatalog.defaults().values().forEach(text ->
                assertThat(BilanMarkup.resolve(text, vars)).doesNotContainPattern("\\{[a-z_]+}"));
    }
}
