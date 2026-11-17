package fr.mtpilates.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class MemberDetailControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachOpensTheFicheOfTheirMember() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coached(coach);
        jdbc.update("UPDATE profiles SET coaching_motivation = 'Reprendre le sport' WHERE id = ?", member.id());

        mvc.perform(get("/app/coach/members/{id}", member.id()).with(coach.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fiche membre")))
                .andExpect(content().string(containsString("Reprendre le sport")))
                .andExpect(content().string(containsString("Notes du coach (priv")));
    }

    @Test
    void coachCannotOpenTheFicheOfAnotherCoachMember() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/members/{id}", other.id()).with(coach.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void coachSavesNotesAndSummaryButNotTheMembership() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coached(coach);

        mvc.perform(post("/app/coach/members/{id}", member.id()).header("HX-Request", "true")
                        .with(coach.login()).with(csrf())
                        .param("goals", "Gainage").param("coachNotes", "Genou droit")
                        .param("objectives", "Posture").param("allergies", "Arachide")
                        .param("status", "archived").param("subscriptionStartDate", "2026-01-01"))
                .andExpect(header().string("HX-Trigger", containsString("Fiche mise \\u00E0 jour")))
                .andExpect(content().string(containsString("Posture")));

        final Map<String, Object> row = jdbc.queryForMap("SELECT goals, coach_notes, objectives::text AS objectives, status,"
                + " subscription_start_date FROM profiles WHERE id = ?", member.id());
        assertThat(row).containsEntry("goals", "Gainage").containsEntry("coach_notes", "Genou droit")
                .containsEntry("status", "active");
        assertThat(row.get("objectives").toString()).contains("Posture");
        assertThat(row.get("subscription_start_date")).isNull();
    }

    @Test
    void managerAlsoSavesTheMembership() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(post("/app/coach/members/{id}", member.id()).header("HX-Request", "true")
                        .with(owner.login()).with(csrf())
                        .param("status", "paused").param("coachId", "none").param("subscriptionEndDate", "2027-06-30"))
                .andExpect(status().isOk());

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, subscription_end_date::text AS ends FROM profiles WHERE id = ?", member.id());
        assertThat(row).containsEntry("status", "paused").containsEntry("ends", "2027-06-30");
    }

    private TestUsers.Account coached(final TestUsers.Account coach) {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        return member;
    }
}
