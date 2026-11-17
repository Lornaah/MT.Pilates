package fr.mtpilates.shared.files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class FileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void unknownBucketAnswersNotFound() throws Exception {
        mvc.perform(get("/files/secret/a/b.pdf")).andExpect(status().isNotFound());
    }

    @Test
    void missingFileAnswersNotFound() throws Exception {
        mvc.perform(get("/files/avatars/inconnu/avatar.png")).andExpect(status().isNotFound());
    }

    @Test
    void pathTraversalIsRejectedBeforeReachingStorage() throws Exception {
        mvc.perform(get("/files/avatars/../../etc/passwd")).andExpect(status().is4xxClientError());
    }
}
