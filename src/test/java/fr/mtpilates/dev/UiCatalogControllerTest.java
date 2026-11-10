package fr.mtpilates.dev;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@ActiveProfiles("dev")
class UiCatalogControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void appliesAThemeWithoutChangingTheTemplate() throws Exception {
        mvc.perform(get("/dev/ui").param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme=\"dark\"")))
                .andExpect(content().string(containsString("Catalogue UI")));
    }

    @Test
    void fallsBackToTheDefaultThemeForAnUnknownOne() throws Exception {
        mvc.perform(get("/dev/ui").param("theme", "inconnu"))
                .andExpect(content().string(containsString("data-theme=\"default\"")));
    }

    @Test
    void sendsToastsThroughTheHtmxTriggerHeader() throws Exception {
        mvc.perform(post("/dev/ui/toast").param("category", "success").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("HX-Trigger", containsString("\"message\":\"Fiche mise \\u00E0 jour\"")))
                .andExpect(header().string("HX-Trigger", containsString("\"category\":\"success\"")));
    }

    @Test
    void rejectsStateChangingRequestsWithoutCsrfToken() throws Exception {
        mvc.perform(post("/dev/ui/toast").param("category", "success"))
                .andExpect(status().isForbidden());
    }
}
