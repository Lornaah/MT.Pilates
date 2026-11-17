package fr.mtpilates.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class MembersControllerTest {

    private static final String STRONG_PASSWORD = "Abcdefghijklmnop-1234567890!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachListsOnlyTheirOwnMembers() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account mine = member("mienne", coach);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/members").with(coach.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mes coachés")))
                .andExpect(content().string(containsString(mine.email())))
                .andExpect(content().string(not(containsString(other.email()))));
    }

    @Test
    void coachOnTheAdminListStillSeesOnlyWhatTheDatabaseRulesAllow() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(get("/app/admin/members").with(coach.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(other.email()))))
                .andExpect(content().string(not(containsString("Nouveau membre"))));
    }

    @Test
    void memberCannotOpenStaffLists() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/admin/members").with(member.login())).andExpect(status().is3xxRedirection());
    }

    @Test
    void filtersBySearchAndStatusInAFragment() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account paused = users.create("enpause", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET status = 'paused' WHERE id = ?", paused.id());
        final TestUsers.Account active = users.create("enpause", AppRole.MEMBER);

        mvc.perform(get("/app/admin/members").param("search", "ENPAUSE").param("status", "paused")
                        .header("HX-Request", "true").with(owner.login()))
                .andExpect(content().string(containsString(paused.email())))
                .andExpect(content().string(not(containsString(active.email()))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void ownerCreatesAMemberAccount() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final String email = "nouveau-" + owner.id().toString().substring(0, 8) + "@example.com";

        mvc.perform(post("/app/admin/members").header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("email", "  " + email.toUpperCase() + " ").param("password", STRONG_PASSWORD)
                        .param("firstName", "Nina").param("lastName", "Leroy"))
                .andExpect(header().string("HX-Trigger", containsString("Membre cr\\u00E9\\u00E9")))
                .andExpect(header().string("HX-Trigger", containsString("create-member")))
                .andExpect(content().string(containsString(email)));

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT p.first_name, r.role FROM profiles p JOIN user_roles r ON r.user_id = p.id WHERE p.email = ?", email);
        assertThat(row).containsEntry("first_name", "Nina").containsEntry("role", "member");
    }

    @Test
    void invalidCreationIsReportedInAToastWithoutSwapping() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);

        mvc.perform(post("/app/admin/members").header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("email", "pas-un-email").param("password", "court").param("firstName", ""))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Email invalide")))
                .andExpect(header().string("HX-Trigger", containsString("Pr\\u00E9nom requis")));
    }

    @Test
    void duplicateEmailIsRefused() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account existing = users.create("existant", AppRole.MEMBER);

        mvc.perform(post("/app/admin/members").header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("email", existing.email()).param("password", STRONG_PASSWORD).param("firstName", "X"))
                .andExpect(header().string("HX-Trigger", containsString("existe d\\u00E9j\\u00E0")));
    }

    @Test
    void coachCannotCreateAccounts() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/admin/members").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("email", "x@example.com").param("password", STRONG_PASSWORD).param("firstName", "X"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'x@example.com'", Integer.class)).isZero();
    }

    @Test
    void ownerUpdatesMembershipFields() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/admin/members/{id}/edit", member.id()).with(owner.login()))
                .andExpect(content().string(containsString("Modifier Membre Test")));
        mvc.perform(post("/app/admin/members/{id}", member.id()).header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("status", "paused").param("coachId", coach.id().toString())
                        .param("subscriptionStartDate", "2026-01-15").param("subscriptionEndDate", "")
                        .param("phone", "0611223344"))
                .andExpect(header().string("HX-Trigger", containsString("Fiche mise \\u00E0 jour")));

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, coach_id::text AS coach, subscription_start_date::text AS start, subscription_end_date, phone"
                        + " FROM profiles WHERE id = ?", member.id());
        assertThat(row).containsEntry("status", "paused").containsEntry("coach", coach.id().toString())
                .containsEntry("start", "2026-01-15").containsEntry("phone", "0611223344");
        assertThat(row.get("subscription_end_date")).isNull();
    }

    @Test
    void coachCannotEditAMemberOfAnotherCoach() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(post("/app/admin/members/{id}", other.id()).header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("status", "archived"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM profiles WHERE id = ?", String.class, other.id())).isEqualTo("active");
    }

    @Test
    void deletingRemovesTheWholeAccountButNeverAnOwner() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final TestUsers.Account otherOwner = users.create("autreowner", AppRole.OWNER);

        mvc.perform(delete("/app/admin/members/{id}", member.id()).header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Membre supprim\\u00E9")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Integer.class, member.id())).isZero();

        mvc.perform(delete("/app/admin/members/{id}", otherOwner.id()).header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Integer.class, otherOwner.id())).isOne();
    }

    private TestUsers.Account member(final String prefix, final TestUsers.Account coach) {
        final TestUsers.Account account = users.create(prefix, AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), account.id());
        return account;
    }
}
