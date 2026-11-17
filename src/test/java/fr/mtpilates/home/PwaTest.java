package fr.mtpilates.home;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Application installable : manifest, service worker et page hors ligne servis sans connexion. */
@IntegrationTest
class PwaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void installableAssetsArePublic() throws Exception {
        mvc.perform(get("/manifest.json")).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"start_url\": \"/app\"")));
        mvc.perform(get("/sw.js")).andExpect(status().isOk()).andExpect(content().string(containsString("/offline.html")));
        mvc.perform(get("/offline.html")).andExpect(status().isOk()).andExpect(content().string(containsString("location.reload()")));
    }
}
