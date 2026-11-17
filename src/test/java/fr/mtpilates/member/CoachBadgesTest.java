package fr.mtpilates.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoachBadgesTest {

    @Test
    void picksTheSamePaletteEntryAsTheOriginalHash() {
        // Index attendus calculés avec getCoachBadgeClass de l'original (2 et 1).
        assertThat(CoachBadges.classFor(Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000002"))))
                .isEqualTo("bg-setter/15 text-setter");
        assertThat(CoachBadges.classFor(Optional.of(UUID.fromString("9f1c2d3e-4b5a-6978-8a9b-0c1d2e3f4a5b"))))
                .isEqualTo("bg-secondary/15 text-secondary");
    }

    @Test
    void noCoachIsMuted() {
        assertThat(CoachBadges.classFor(Optional.empty())).isEqualTo("bg-muted text-muted-foreground");
    }
}
