package fr.mtpilates.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class SessionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void savesBlocksTransactionallyAndDropsEmptySlots() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID exercise = exercise();
        final String payload = "{\"title\":\"Séance A\",\"level\":\"beginner\",\"is_template\":true,\"blocks\":["
                + "{\"block_type\":\"superset\",\"rounds\":0,\"exercises\":[{\"exercise_id\":\"" + exercise + "\",\"sets\":4,"
                + "\"attributes\":[{\"type\":\"reps\",\"default\":12}]},{\"exercise_id\":\"\",\"sets\":3,\"attributes\":[]}]},"
                + "{\"block_type\":\"single\",\"rounds\":1,\"exercises\":[{\"exercise_id\":\"\"}]}]}";

        final MvcResult result = mvc.perform(post("/app/coach/sessions/save").header("HX-Request", "true")
                        .with(coach.login()).with(csrf()).param("payload", payload))
                .andExpect(header().string("HX-Redirect", startsWith("/app/coach/sessions/")))
                .andReturn();
        final UUID id = UUID.fromString(result.getResponse().getHeader("HX-Redirect").substring("/app/coach/sessions/".length()));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_session_blocks WHERE session_id = ?", Integer.class, id)).isOne();
        assertThat(jdbc.queryForObject("SELECT rounds FROM workout_session_blocks WHERE session_id = ?", Integer.class, id)).isOne();
        assertThat(jdbc.queryForObject("SELECT e.sets FROM workout_session_exercises e JOIN workout_session_blocks b ON b.id = e.block_id"
                + " WHERE b.session_id = ?", Integer.class, id)).isEqualTo(4);

        final String update = payload.replace("Séance A", "Séance B").replace("\"sets\":4", "\"sets\":2");
        mvc.perform(post("/app/coach/sessions/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                .param("id", id.toString()).param("payload", update));
        assertThat(jdbc.queryForObject("SELECT title FROM workout_sessions WHERE id = ?", String.class, id)).isEqualTo("Séance B");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_session_blocks WHERE session_id = ?", Integer.class, id)).isOne();
    }

    @Test
    void refusesUntitledOrEmptySessions() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/sessions/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("payload", "{\"title\":\" \",\"blocks\":[]}"))
                .andExpect(header().string("HX-Trigger", containsString("Le titre est requis")));
        mvc.perform(post("/app/coach/sessions/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("payload", "{\"title\":\"Vide\",\"blocks\":[{\"block_type\":\"single\",\"exercises\":[]}]}"))
                .andExpect(header().string("HX-Trigger", containsString("Ajoutez au moins un exercice valide")));
    }

    @Test
    void duplicateCopiesBlocksAndOpensTheCopy() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID exercise = exercise();
        final MvcResult saved = mvc.perform(post("/app/coach/sessions/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                .param("payload", "{\"title\":\"Source\",\"blocks\":[{\"block_type\":\"single\",\"exercises\":[{\"exercise_id\":\"" + exercise + "\"}]}]}"))
                .andReturn();
        final String source = saved.getResponse().getHeader("HX-Redirect").substring("/app/coach/sessions/".length());

        final MvcResult copy = mvc.perform(post("/app/coach/sessions/{id}/duplicate", source).header("HX-Request", "true")
                .with(coach.login()).with(csrf())).andReturn();
        final UUID copyId = UUID.fromString(copy.getResponse().getHeader("HX-Redirect").substring("/app/coach/sessions/".length()));
        assertThat(jdbc.queryForObject("SELECT title FROM workout_sessions WHERE id = ?", String.class, copyId)).isEqualTo("Source (copie)");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises e JOIN workout_session_blocks b ON b.id = e.block_id"
                + " WHERE b.session_id = ?", Integer.class, copyId)).isOne();
        mvc.perform(get("/app/coach/sessions/{id}", copyId).with(coach.login()))
                .andExpect(content().string(containsString("Modifier la séance")));
    }

    @Test
    void membersCannotOpenTheEditor() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/sessions/new").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    private UUID exercise() {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO exercises (id, title) VALUES (?, 'Exo')", id);
        return id;
    }
}
