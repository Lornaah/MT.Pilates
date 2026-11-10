package fr.mtpilates.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.CapturingMailSender;
import fr.mtpilates.support.IntegrationTest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@TestPropertySource(properties = "app.auth.owner-emails=boss@example.com")
class RegistrationFlowTest {

    private static final String STRONG = "UnMotDePasseTresSolide2026!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CapturingMailSender mails;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        mails.clear();
    }

    @Test
    void createsTheAccountOnlyAfterTheEmailIsConfirmed() throws Exception {
        final String email = unique("lea");

        signup(email, STRONG)
                .andExpect(status().isNoContent())
                .andExpect(header().string("HX-Trigger", containsString("\"registered\":true")));

        assertThat(countUsers(email)).isZero();
        assertThat(mails.sent()).hasSize(1);
        assertThat(mails.sent().get(0).getSubject()).isEqualTo(RegistrationMailer.SUBJECT);

        mvc.perform(get(pathOf(mails.lastConfirmationLink())))
                .andExpect(redirectedUrl("/app"));

        assertThat(countUsers(email)).isOne();
        assertThat(rolesOf(email)).isEqualTo("member");
    }

    @Test
    void normalizesTheEmailBeforeRegistering() throws Exception {
        final String email = unique("first.last");

        signup("  " + email.toUpperCase() + " ", STRONG).andExpect(status().isNoContent());
        mvc.perform(get(pathOf(mails.lastConfirmationLink())));

        assertThat(countUsers(email)).isOne();
    }

    @Test
    void grantsOwnerToTheConfiguredEmails() throws Exception {
        signup("boss@example.com", STRONG);
        mvc.perform(get(pathOf(mails.lastConfirmationLink())));

        assertThat(rolesOf("boss@example.com")).isEqualTo("owner");
    }

    @Test
    void reportsEveryValidationErrorInOneToast() throws Exception {
        signup("pas-un-email", "court")
                .andExpect(header().string("HX-Trigger", containsString(
                        "Email invalide \\u2022 24 caract\\u00E8res minimum")));

        assertThat(mails.sent()).isEmpty();
    }

    @Test
    void refusesAnEmailThatAlreadyHasAnAccount() throws Exception {
        final String email = unique("deja");
        signup(email, STRONG);
        mvc.perform(get(pathOf(mails.lastConfirmationLink())));

        signup(email, STRONG)
                .andExpect(header().string("HX-Trigger", containsString("Un compte existe d\\u00E9j\\u00E0 avec cet email")));
    }

    @Test
    void rejectsAnExpiredLink() throws Exception {
        final String email = unique("tard");
        signup(email, STRONG);
        jdbc.update("UPDATE pending_registrations SET expires_at = ? WHERE lower(email) = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), email);

        mvc.perform(get(pathOf(mails.lastConfirmationLink())))
                .andExpect(redirectedUrl("/auth"));
        assertThat(countUsers(email)).isZero();
    }

    @Test
    void resendingInvalidatesThePreviousLink() throws Exception {
        final String email = unique("renvoi");
        signup(email, STRONG);
        final String firstLink = mails.lastConfirmationLink();

        mvc.perform(post("/auth/resend").param("email", email).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("renvoy\\u00E9")));

        mvc.perform(get(pathOf(firstLink))).andExpect(redirectedUrl("/auth"));
        mvc.perform(get(pathOf(mails.lastConfirmationLink()))).andExpect(redirectedUrl("/app"));
    }

    private org.springframework.test.web.servlet.ResultActions signup(final String email, final String password) throws Exception {
        return mvc.perform(post("/auth/signup").header("HX-Request", "true")
                .param("email", email).param("password", password).with(csrf()));
    }

    private int countUsers(final String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(email) = lower(?)", Integer.class, email);
    }

    private String rolesOf(final String email) {
        return jdbc.queryForObject("SELECT string_agg(role, ',') FROM user_roles r JOIN users u ON u.id = r.user_id "
                + "WHERE lower(u.email) = lower(?)", String.class, email);
    }

    private static String unique(final String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String pathOf(final String link) {
        final URI uri = URI.create(link);
        return uri.getPath() + "?" + uri.getQuery();
    }
}
