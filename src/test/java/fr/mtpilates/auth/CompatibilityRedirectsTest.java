package fr.mtpilates.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Redirections de compatibilité de l'original (App.tsx), toutes au même endroit. */
@IntegrationTest
class CompatibilityRedirectsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void oldUrlsLeadToTheirNewPlace() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER, AppRole.COACH, AppRole.SETTER);
        final Map<String, String> redirects = Map.of(
                "/app/coach/recipes", "/app/coach/nutrition?tab=recipes",
                "/app/coach/foods", "/app/coach/nutrition?tab=foods",
                "/app/coach/meal-plans/generate", "/app/coach/nutrition/plans/generate",
                "/app/admin/prospects", "/app/prospects",
                "/app/admin/finance", "/app/money");
        for (final Map.Entry<String, String> r : redirects.entrySet()) {
            mvc.perform(get(r.getKey()).with(owner.login())).andExpect(redirectedUrl(r.getValue()));
        }
    }
}
