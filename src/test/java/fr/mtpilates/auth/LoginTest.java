package fr.mtpilates.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

@IntegrationTest
class LoginTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void welcomesTheUserAndLandsOnTheApp() throws Exception {
        final TestUsers.Account account = users.create("coach", AppRole.COACH);

        final MvcResult result = login(account.email().toUpperCase(), TestUsers.PASSWORD)
                .andExpect(redirectedUrl("/app")).andReturn();

        assertThat(toastOf(result).message()).isEqualTo("Bienvenue !");
    }

    @Test
    void doesNotRevealWhetherTheAccountExists() throws Exception {
        final TestUsers.Account account = users.create("membre", AppRole.MEMBER);

        final MvcResult wrongPassword = login(account.email(), "mauvais").andExpect(redirectedUrl("/auth")).andReturn();
        final MvcResult unknown = login("inconnu@example.com", "mauvais").andReturn();

        assertThat(toastOf(wrongPassword).message()).isEqualTo(LoginFailureHandler.INVALID_CREDENTIALS);
        assertThat(toastOf(unknown).message()).isEqualTo(LoginFailureHandler.INVALID_CREDENTIALS);
    }

    @Test
    void reportsFormErrorsLikeTheOriginalValidation() throws Exception {
        assertThat(toastOf(login("", "x").andReturn()).message()).isEqualTo(EmailRules.REQUIRED);
        assertThat(toastOf(login("pas-un-email", "x").andReturn()).message()).isEqualTo(EmailRules.INVALID);
        assertThat(toastOf(login("a@example.com", "").andReturn()).message()).isEqualTo(LoginFailureHandler.PASSWORD_REQUIRED);
    }

    @Test
    void keepsTheNormalizedEmailAfterAFailure() throws Exception {
        final MvcResult result = login("Quelqu.Un@Example.com", "mauvais").andReturn();

        assertThat(flashOf(result).get("email")).isEqualTo("quelqu.un@example.com");
    }

    private ResultActions login(final String email, final String password) throws Exception {
        return mvc.perform(post("/auth/login").param("email", email).param("password", password).with(csrf()));
    }

    private static Toast toastOf(final MvcResult result) {
        return (Toast) flashOf(result).get("toast");
    }

    /** Les handlers Spring Security écrivent le flash directement en session : on le relit comme la requête suivante. */
    private static FlashMap flashOf(final MvcResult result) {
        final MockHttpServletRequest next = new MockHttpServletRequest("GET", result.getResponse().getRedirectedUrl());
        next.setSession(result.getRequest().getSession());
        final FlashMap flash = new SessionFlashMapManager().retrieveAndUpdate(next, new MockHttpServletResponse());
        assertThat((Object) flash).as("flash posé pour la page suivante").isNotNull();
        return flash;
    }
}
