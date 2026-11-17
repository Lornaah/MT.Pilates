package fr.mtpilates.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class VideoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void membersSeeOnlyPublishedVideos() throws Exception {
        final TestUsers.Account member = users.create("video", AppRole.MEMBER);
        final String suffix = member.id().toString().substring(0, 8);
        insertVideo("Publiée " + suffix, true);
        insertVideo("Brouillon " + suffix, false);

        mvc.perform(get("/app/videos").with(member.login()))
                .andExpect(content().string(containsString("Publiée " + suffix)))
                .andExpect(content().string(not(containsString("Brouillon " + suffix))));
    }

    @Test
    void openingAVideoRecordsAView() throws Exception {
        final TestUsers.Account member = users.create("video", AppRole.MEMBER);
        final UUID video = insertVideo("Vue", true);

        mvc.perform(post("/app/videos/{id}/views", video).with(member.login()).with(csrf())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_views WHERE user_id = ? AND video_id = ?", Integer.class,
                member.id(), video)).isOne();
    }

    @Test
    void staffAddsAndDeletesVideos() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Séance " + coach.id().toString().substring(0, 8);

        mvc.perform(post("/app/coach/videos").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", title).param("youtube", "https://youtu.be/abcdefghijk").param("level", "advanced")
                        .param("focus", " dos, ,cou ").param("duration", "30"))
                .andExpect(header().string("HX-Trigger", containsString("S\\u00E9ance ajout\\u00E9e")))
                .andExpect(content().string(containsString(title)));
        final UUID id = jdbc.queryForObject("SELECT id FROM videos WHERE title = ?", UUID.class, title);
        assertThat(jdbc.queryForObject("SELECT youtube_id || '|' || level || '|' || array_to_string(focus_areas, ',') FROM videos WHERE id = ?",
                String.class, id)).isEqualTo("abcdefghijk|advanced|dos,cou");

        mvc.perform(delete("/app/coach/videos/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Supprim\\u00E9e")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM videos WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void invalidVideoIsRefusedWithAToast() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/videos").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", "Sans lien").param("youtube", "pas une url"))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Titre et URL/ID YouTube valide requis")));
    }

    @Test
    void membersCannotManageVideos() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/videos").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void coachSeesTheViewingHistoryOnTheMemberFiche() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        final UUID video = insertVideo("Historique", true);
        jdbc.update("INSERT INTO video_views (user_id, video_id, watched_at, completed, rating) VALUES (?, ?, '2026-11-10T09:00:00Z', true, 3)",
                member.id(), video);

        mvc.perform(get("/app/coach/members/{id}", member.id()).with(coach.login()))
                .andExpect(content().string(containsString("Historique")))
                .andExpect(content().string(containsString("Complétée")))
                .andExpect(content().string(containsString("10 nov. 2026")));
    }

    private UUID insertVideo(final String title, final boolean published) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO videos (id, title, youtube_id, is_published) VALUES (?, ?, 'abcdefghijk', ?)", id, title, published);
        return id;
    }
}
