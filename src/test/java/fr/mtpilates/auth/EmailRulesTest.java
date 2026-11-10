package fr.mtpilates.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailRulesTest {

    @Test
    void acceptsDotsPlusAddressingAndSubdomains() {
        // TODO.md de l'original : les emails avec un « . » posaient problème à la connexion.
        assertThat(EmailRules.firstError("first.last@example.com")).isEmpty();
        assertThat(EmailRules.firstError("me+tag@mail.example.fr")).isEmpty();
    }

    @Test
    void reportsTheFirstErrorLikeZod() {
        assertThat(EmailRules.firstError("")).contains(EmailRules.REQUIRED);
        assertThat(EmailRules.firstError("pas-un-email")).contains(EmailRules.INVALID);
        assertThat(EmailRules.firstError("sans@tld")).contains(EmailRules.INVALID);
        assertThat(EmailRules.firstError("a@" + "b".repeat(251) + ".fr")).contains(EmailRules.TOO_LONG);
    }
}
