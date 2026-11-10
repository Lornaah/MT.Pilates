package fr.mtpilates.home;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class LandingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rendersTheLandingPageWithItsSections() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Le Pilates qui vous")))
                .andExpect(content().string(containsString("Bibliothèque vidéo")))
                .andExpect(content().string(containsString("Prête à retrouver votre élan")))
                .andExpect(content().string(containsString("data-theme=\"default\"")));
    }

    @Test
    void rendersTheNotFoundPageForUnknownUrls() throws Exception {
        mvc.perform(get("/nimporte-quoi"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthenticationForTheMemberArea() throws Exception {
        mvc.perform(get("/app"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void servesTheBuiltStylesheet() throws Exception {
        mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--primary")));
    }
}
