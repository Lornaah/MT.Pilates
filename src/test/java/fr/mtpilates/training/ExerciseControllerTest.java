package fr.mtpilates.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ExerciseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachCreatesAnExerciseWithCleanedAttributes() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Fente " + coach.id().toString().substring(0, 8);
        final String attributes = "[{\"type\":\"reps\",\"default\":\"12\"},{\"type\":\"inconnu\"},{\"type\":\"weight\",\"default\":7.5},"
                + "{\"type\":\"rest\",\"default\":60},{\"type\":\"amrap\",\"default\":true},{\"type\":\"na\"},{\"type\":\"kcal\",\"default\":5}]";

        mvc.perform(post("/app/coach/exercises").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", "  " + title + " ").param("muscleGroups", "Jambes", "Fessiers")
                        .param("exerciseType", "strength").param("defaultAttributes", attributes).param("published", "false", "true"))
                .andExpect(header().string("HX-Trigger", containsString("Exercice cr\\u00E9\\u00E9")))
                .andExpect(content().string(containsString(title)));

        final Map<String, Object> row = jdbc.queryForMap("SELECT default_attributes::text AS attrs, is_published, created_by,"
                + " array_to_string(muscle_groups, ',') AS muscles FROM exercises WHERE title = ?", title);
        assertThat(row.get("attrs").toString())
                .isEqualTo("[{\"type\": \"reps\", \"default\": 12}, {\"type\": \"weight\", \"unit\": \"kg\", \"default\": 7.5}, "
                        + "{\"type\": \"rest\", \"unit\": \"s\", \"default\": 60}, {\"type\": \"amrap\", \"default\": true}, "
                        + "{\"type\": \"na\", \"default\": null}]");
        assertThat(row).containsEntry("is_published", true).containsEntry("created_by", coach.id())
                .containsEntry("muscles", "Jambes,Fessiers");
    }

    @Test
    void untitledExerciseIsIgnored() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/exercises").header("HX-Request", "true").with(coach.login()).with(csrf()).param("title", " "))
                .andExpect(header().string("HX-Reswap", "none"));
    }

    @Test
    void duplicateAddsACopyWithoutObjectivesNorFavorite() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID id = UUID.randomUUID();
        final String title = "Original " + id.toString().substring(0, 8);
        jdbc.update("INSERT INTO exercises (id, title, objectives, is_favorite) VALUES (?, ?, '{Tonicité}', true)", id, title);

        mvc.perform(post("/app/coach/exercises/{id}/duplicate", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Exercice dupliqu\\u00E9")));
        final Map<String, Object> copy = jdbc.queryForMap(
                "SELECT cardinality(objectives) AS n, is_favorite, created_by FROM exercises WHERE title = ?", title + " (copie)");
        assertThat(copy).containsEntry("n", 0).containsEntry("is_favorite", false).containsEntry("created_by", coach.id());
    }

    @Test
    void favoriteToggles() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO exercises (id, title) VALUES (?, 'Fav')", id);

        mvc.perform(post("/app/coach/exercises/{id}/favorite", id).header("HX-Request", "true").with(coach.login()).with(csrf()));
        assertThat(jdbc.queryForObject("SELECT is_favorite FROM exercises WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void hubShowsTheExercisesTabAndMembersAreKeptOut() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/training").param("tab", "exercises").with(coach.login()))
                .andExpect(content().string(containsString("Bibliothèque d'exercices")));
        mvc.perform(get("/app/coach/training").with(member.login())).andExpect(redirectedUrl("/app"));
    }
}
