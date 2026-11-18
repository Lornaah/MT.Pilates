package fr.mtpilates.bilan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BilanChartsTest {

    private static List<BilanCalculator.WeekPoint> weights(final double... values) {
        return java.util.Arrays.stream(values).mapToObj(v -> new BilanCalculator.WeekPoint("s", v)).toList();
    }

    @Test
    void weightAxisIsAWindowOfTenKilosAroundTheStartingWeight() {
        final double[] range = BilanCharts.range(weights(63.2, 62.5, 59.9), BilanCharts.Style.WEIGHT, Optional.empty());

        assertThat(range[0]).isLessThanOrEqualTo(53.2).isGreaterThanOrEqualTo(50);
        assertThat(range[1]).isGreaterThanOrEqualTo(73.2).isLessThanOrEqualTo(75);
    }

    @Test
    void weightAxisWidensToShowABigLossAndTheGoal() {
        final double[] range = BilanCharts.range(weights(80, 72, 68), BilanCharts.Style.WEIGHT, Optional.of(60.0));

        assertThat(range[0]).isLessThanOrEqualTo(60);
        assertThat(range[1]).isGreaterThanOrEqualTo(90);
    }
}
