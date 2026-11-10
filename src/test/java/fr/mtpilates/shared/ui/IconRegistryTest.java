package fr.mtpilates.shared.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IconRegistryTest {

    private final IconRegistry icons = new IconRegistry();

    @Test
    void rendersAnInlineSvgWithTheRequestedClasses() {
        final String svg = icons.svg("calendar", "h-4 w-4");

        assertThat(svg)
                .startsWith("<svg")
                .contains("class=\"lucide lucide-calendar h-4 w-4\"")
                .contains("aria-hidden=\"true\"")
                .doesNotContain("@license");
    }

    @Test
    void escapesClassesToAvoidAttributeInjection() {
        assertThat(icons.svg("calendar", "\" onload=\"x")).doesNotContain("\" onload=\"");
    }

    @Test
    void rejectsUnknownOrMalformedNames() {
        assertThatThrownBy(() -> icons.svg("n-existe-pas", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> icons.svg("../secret", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
