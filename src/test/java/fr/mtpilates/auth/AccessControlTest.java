package fr.mtpilates.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Gardes de routes de l'original (ProtectedRoute, StaffGate, OwnerGate) et « Voir comme ». */
@IntegrationTest
class AccessControlTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void sendsAnonymousVisitorsToTheLoginPage() throws Exception {
        mvc.perform(get("/app/admin/team")).andExpect(redirectedUrl("/auth"));
        mvc.perform(get("/app").header("HX-Request", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("HX-Redirect", "/auth"));
    }

    @Test
    void sendsSignedInUsersAwayFromTheLandingAndLoginPages() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/").with(member.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(get("/auth").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void keepsMembersOutOfStaffPages() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/members").with(member.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app/prospects").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void letsEveryStaffRoleOfTheInterfaceThroughTheStaffGate() throws Exception {
        for (final AppRole role : new AppRole[] {AppRole.COACH, AppRole.SETTER, AppRole.CLOSER, AppRole.MANAGER_COACH}) {
            final TestUsers.Account staff = users.create("staff", role);
            // Pas encore de page derrière : la garde laisse passer, d'où la 404.
            mvc.perform(get("/app/coach/members").with(staff.login())).andExpect(status().isNotFound());
        }
    }

    @Test
    void reservesTeamSettingsAndFinanceToTheOwner() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);

        mvc.perform(get("/app/admin/team").with(coach.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app/money").with(coach.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app/admin/team").with(owner.login())).andExpect(status().isOk());
    }

    @Test
    void ownerViewingAsAnotherRoleGetsThatRoleAccessAndTheBanner() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/app/view-as").param("role", "member").session(session).with(owner.login()).with(csrf()))
                .andExpect(redirectedUrl("/app"));
        mvc.perform(get("/app").session(session).with(owner.login()))
                .andExpect(content().string(containsString("Revenir au mode owner")))
                .andExpect(content().string(containsString("Mon suivi")));
        mvc.perform(get("/app/admin/team").session(session).with(owner.login())).andExpect(redirectedUrl("/app"));

        mvc.perform(post("/app/view-as").param("role", "owner").session(session).with(owner.login()).with(csrf()));
        mvc.perform(get("/app/admin/team").session(session).with(owner.login())).andExpect(status().isOk());
    }

    @Test
    void ignoresViewAsForNonOwners() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/app/view-as").param("role", "member").session(session).with(coach.login()).with(csrf()));

        mvc.perform(get("/app").session(session).with(coach.login()))
                .andExpect(content().string(not(containsString("Revenir au mode owner"))))
                .andExpect(content().string(containsString("Mes coachés")));
    }
}
