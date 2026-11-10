package fr.mtpilates.shared.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatToneTest {

    @Test
    void resolvesTonesByTemplateName() {
        assertThat(StatTone.of("accent").card()).isEqualTo("bg-accent/20");
        assertThat(StatTone.of("sage").icon()).isEqualTo("bg-sage/15 text-sage");
    }

    @Test
    void defaultsWhenNoToneIsGiven() {
        assertThat(StatTone.of(null)).isEqualTo(StatTone.DEFAULT);
        assertThat(StatTone.of(" ")).isEqualTo(StatTone.DEFAULT);
    }
}
