package fr.mtpilates.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void showsTheOwnProfile() throws Exception {
        final TestUsers.Account member = users.create("fiche", AppRole.MEMBER);

        mvc.perform(get("/app/profile").with(member.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vos informations")))
                .andExpect(content().string(containsString("Fiche")));
    }

    @Test
    void savesTheProfileAndConfirmsWithAToast() throws Exception {
        final TestUsers.Account member = users.create("fiche", AppRole.MEMBER);

        mvc.perform(post("/app/profile").header("HX-Request", "true").with(member.login()).with(csrf())
                        .param("firstName", "Léa").param("lastName", "Martin").param("phone", "0601020304")
                        .param("goals", "Posture").param("weeklyTargetSessions", "4")
                        .param("allergies", "Arachide").param("workSedentarity", "active"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("HX-Trigger", containsString("Fiche mise \\u00E0 jour")));

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT phone, goals, weekly_target_sessions, allergies::text AS allergies, work_sedentarity"
                        + " FROM profiles WHERE id = ?", member.id());
        assertThat(row).containsEntry("phone", "0601020304").containsEntry("goals", "Posture")
                .containsEntry("weekly_target_sessions", 4).containsEntry("work_sedentarity", "active");
        assertThat(row.get("allergies").toString()).contains("Arachide");
    }

    @Test
    void uploadsAnAvatarServedPublicly() throws Exception {
        final TestUsers.Account member = users.create("avatar", AppRole.MEMBER);
        final MockMultipartFile file = new MockMultipartFile("file", "moi.png", "image/png", new byte[] {1, 2, 3});

        mvc.perform(multipart("/app/profile/avatar").file(file).header("HX-Request", "true")
                        .with(member.login()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger", containsString("Photo mise \\u00E0 jour")));

        final String url = jdbc.queryForObject("SELECT avatar_url FROM profiles WHERE id = ?", String.class, member.id());
        assertThat(url).startsWith("/files/avatars/" + member.id() + "/avatar.png");
        mvc.perform(get(url.substring(0, url.indexOf('?'))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }
}
