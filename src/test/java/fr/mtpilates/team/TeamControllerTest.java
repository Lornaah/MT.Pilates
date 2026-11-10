package fr.mtpilates.team;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class TeamControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listsOnlyPeopleWithAStaffRole() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coachliste", AppRole.COACH);
        final TestUsers.Account member = users.create("membreliste", AppRole.MEMBER);

        mvc.perform(get("/app/admin/team").with(owner.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coach.email())))
                .andExpect(content().string(not(containsString(member.email()))));
    }

    @Test
    void filtersBySearchAndRoleInAFragmentForHtmx() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account setter = users.create("settersearch", AppRole.SETTER);

        mvc.perform(get("/app/admin/team").param("q", "settersearch").param("role", "setter")
                        .header("HX-Request", "true").with(owner.login()))
                .andExpect(content().string(containsString(setter.email())))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void ownerAddsAndRemovesRoles() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/admin/team/{id}/roles", coach.id()).param("newRole", "manager_coach")
                        .header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("R\\u00F4le ajout\\u00E9")));
        assertThat(roles(coach)).contains("manager_coach");

        mvc.perform(delete("/app/admin/team/{id}/roles/{role}", coach.id(), "manager_coach")
                        .header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("R\\u00F4le retir\\u00E9")));
        assertThat(roles(coach)).doesNotContain("manager_coach");
    }

    @Test
    void neverRemovesTheOwnerRole() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account otherOwner = users.create("autreowner", AppRole.OWNER);

        mvc.perform(delete("/app/admin/team/{id}/roles/{role}", otherOwner.id(), "owner")
                        .header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("ne peut pas \\u00EAtre retir\\u00E9")));
        assertThat(roles(otherOwner)).contains("owner");
    }

    @Test
    void refusesRoleChangesFromNonOwners() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account target = users.create("cible", AppRole.SETTER);

        mvc.perform(post("/app/admin/team/{id}/roles", target.id()).param("newRole", "coach")
                        .with(coach.login()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(roles(target)).doesNotContain("coach");
    }

    private java.util.List<String> roles(final TestUsers.Account account) {
        return jdbc.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, account.id());
    }
}
