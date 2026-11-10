package fr.mtpilates.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsAPasswordMeetingEveryRule() {
        assertThat(PasswordPolicy.violations("UnMotDePasseTresSolide2026!")).isEmpty();
    }

    @Test
    void listsEveryBrokenRuleInTheOriginalOrder() {
        assertThat(PasswordPolicy.violations("court")).containsExactly(
                PasswordPolicy.TOO_SHORT, PasswordPolicy.NO_UPPER, PasswordPolicy.NO_DIGIT, PasswordPolicy.NO_SPECIAL);
    }

    @Test
    void rejectsPasswordsLongerThanWhatBcryptHashes() {
        assertThat(PasswordPolicy.violations("Aa1!" + "x".repeat(69))).containsExactly(PasswordPolicy.TOO_LONG);
    }

    @Test
    void acceptsAnyNonAlphanumericCharacterAsSpecial() {
        assertThat(PasswordPolicy.violations("UnMotDePasseTresSolide2026€")).isEmpty();
    }
}
