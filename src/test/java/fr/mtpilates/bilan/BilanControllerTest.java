package fr.mtpilates.bilan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.CapturingMailSender;
import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class BilanControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CapturingMailSender mails;

    @BeforeEach
    void setUp() {
        mails.clear();
    }

    @Test
    void coachCreatesADraftReadyToReview() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);

        final UUID id = createDraft(coach, member);

        final Map<String, Object> row = jdbc.queryForMap("SELECT status, content::text AS content FROM member_reports WHERE id = ?", id);
        assertThat(row).containsEntry("status", "draft");
        assertThat(row.get("content").toString()).contains("weight_kg").contains("couverture.titre");
        mvc.perform(get("/app/coach/bilans/{id}", id).with(coach.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enregistrer le brouillon")));
        mvc.perform(get("/app/coach/bilans/{id}/pdf", id).with(coach.login()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void coachCannotCreateABilanForAnotherCoachMember() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(post("/app/coach/members/{id}/bilans", other.id()).with(coach.login()).with(csrf()))
                .andExpect(redirectedUrl("/app"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_reports WHERE user_id = ?", Integer.class, other.id()))
                .isZero();
    }

    @Test
    void memberCannotCreateOrOpenTheirOwnDraft() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);
        final UUID id = createDraft(coach, member);

        mvc.perform(post("/app/coach/members/{id}/bilans", member.id()).with(member.login()).with(csrf()))
                .andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app/coach/bilans/{id}", id).with(member.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app/coach/bilans/{id}/pdf", id).with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void savingADraftKeepsTheSettingsAndRejectsAnInvertedPeriod() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);
        final UUID id = createDraft(coach, member);

        save(coach, id, "2026-09-01", "2026-10-01", "55")
                .andExpect(header().string("HX-Trigger", containsString("bilan-saved")));
        assertThat(jdbc.queryForObject("SELECT target_weight_kg FROM member_reports WHERE id = ?", Double.class, id))
                .isEqualTo(55.0);

        save(coach, id, "2026-10-01", "2026-09-01", "55")
                .andExpect(header().string("HX-Trigger", containsString("La date de fin doit suivre")));
    }

    @Test
    void sendingMailsThePdfAndUnlocksItForTheMemberOnly() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);
        final TestUsers.Account stranger = users.create("autre", AppRole.MEMBER);
        final UUID id = createDraft(coach, member);

        mvc.perform(get("/app/bilans/{id}/pdf", id).with(member.login())).andExpect(status().isNotFound());

        mvc.perform(post("/app/coach/bilans/{id}/send", id).with(coach.login()).with(csrf())
                        .param("periodStart", "2026-09-01").param("periodEnd", "2026-10-01").param("targetWeight", "55")
                        .param("content", currentContent(id)))
                .andExpect(header().string("HX-Redirect", "/app/coach/members/" + member.id() + "?bilans"));

        assertThat(jdbc.queryForObject("SELECT status FROM member_reports WHERE id = ?", String.class, id)).isEqualTo("sent");
        assertThat(mails.sent()).hasSize(1);
        final MimeMessage mail = mails.sent().get(0);
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo(member.email());
        assertThat(((MimeMultipart) mail.getContent()).getCount()).isGreaterThan(1);

        mvc.perform(get("/app/bilans/{id}/pdf", id).with(member.login()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));
        mvc.perform(get("/app/bilans/{id}/pdf", id).with(stranger.login())).andExpect(status().isNotFound());
        mvc.perform(get("/app/tracking").with(member.login()))
                .andExpect(content().string(containsString("Mes bilans")));
    }

    @Test
    void coachEditsASentBilanAndPublishesTheUpdateWithOrWithoutEmail() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);
        final UUID id = createDraft(coach, member);
        send(coach, id, "55", false);
        final byte[] sentPdf = download(member, id);
        mails.clear();

        save(coach, id, "2026-09-01", "2026-10-01", "50")
                .andExpect(header().string("HX-Trigger", containsString("Modifications enregistr")));
        assertThat(download(member, id)).isEqualTo(sentPdf);

        send(coach, id, "50", false);
        assertThat(mails.sent()).isEmpty();
        assertThat(download(member, id)).isNotEqualTo(sentPdf);

        send(coach, id, "50", true);
        assertThat(mails.sent()).hasSize(1);
    }

    private void send(final TestUsers.Account coach, final UUID id, final String target, final boolean notify) throws Exception {
        mvc.perform(post("/app/coach/bilans/{id}/send", id).with(coach.login()).with(csrf())
                        .param("periodStart", "2026-09-01").param("periodEnd", "2026-10-01").param("targetWeight", target)
                        .param("notify", notify ? "true" : "").param("content", currentContent(id)))
                .andExpect(header().exists("HX-Redirect"));
    }

    private byte[] download(final TestUsers.Account member, final UUID id) throws Exception {
        return mvc.perform(get("/app/bilans/{id}/pdf", id).with(member.login()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    @Test
    void coachDeletesADraft() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coachedWithMeasures(coach);
        final UUID id = createDraft(coach, member);

        mvc.perform(delete("/app/coach/bilans/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aucun bilan")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_reports WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void ownerEditsAndResetsTheTemplate() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(get("/app/owner/bilan-template").with(coach.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(post("/app/owner/bilan-template").header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("fierte.titre", "BRAVO {prenom}"))
                .andExpect(header().string("HX-Trigger", containsString("Mod\\u00E8le de bilan enregistr\\u00E9")));
        assertThat(jdbc.queryForObject("SELECT value::text FROM app_config WHERE key = 'bilan_template'", String.class))
                .contains("BRAVO {prenom}");

        mvc.perform(post("/app/owner/bilan-template/reset").header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/app/owner/bilan-template").with(owner.login()))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("BRAVO {prenom}"))));
    }

    private UUID createDraft(final TestUsers.Account coach, final TestUsers.Account member) throws Exception {
        final MvcResult result = mvc.perform(post("/app/coach/members/{id}/bilans", member.id())
                        .header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Redirect", startsWith("/app/coach/bilans/")))
                .andReturn();
        return UUID.fromString(result.getResponse().getHeader("HX-Redirect").substring("/app/coach/bilans/".length()));
    }

    private org.springframework.test.web.servlet.ResultActions save(final TestUsers.Account coach, final UUID id,
                                                                    final String start, final String end,
                                                                    final String target) throws Exception {
        return mvc.perform(post("/app/coach/bilans/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf())
                .param("periodStart", start).param("periodEnd", end).param("targetWeight", target)
                .param("content", currentContent(id)));
    }

    private String currentContent(final UUID id) {
        return jdbc.queryForObject("SELECT content::text FROM member_reports WHERE id = ?", String.class, id);
    }

    private TestUsers.Account coachedWithMeasures(final TestUsers.Account coach) {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ?, first_name = 'Léa' WHERE id = ?", coach.id(), member.id());
        final LocalDate today = LocalDate.now();
        jdbc.update("INSERT INTO member_tracking (user_id, date, weight_kg, waist_cm) VALUES (?, ?, 62, 80)",
                member.id(), today.minusDays(30));
        jdbc.update("INSERT INTO member_tracking (user_id, date, weight_kg, waist_cm) VALUES (?, ?, 59.5, 76)",
                member.id(), today.minusDays(2));
        return member;
    }
}
