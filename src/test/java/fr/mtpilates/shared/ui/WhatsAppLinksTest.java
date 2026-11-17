package fr.mtpilates.shared.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhatsAppLinksTest {

    @Test
    void encodesLikeEncodeUriComponent() {
        assertThat(WhatsAppLinks.link("+33 6 12-34", "Êtes-vous intéressé(e) ? l'essai !"))
                .isEqualTo("https://wa.me/3361234?text=%C3%8Ates-vous%20int%C3%A9ress%C3%A9(e)%20%3F%20l'essai%20!");
    }
}
