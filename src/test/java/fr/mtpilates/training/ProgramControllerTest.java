package fr.mtpilates.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ProgramControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void savesWeeksDaysAndOrderedSessions() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID s1 = session("Une");
        final UUID s2 = session("Deux");
        final String payload = "{\"title\":\"Prog\",\"objectives\":[\"Tonicité\"],\"weeks\":[{\"week_comment\":\"S1\",\"days\":["
                + "{\"day_of_week\":1,\"day_type\":\"mixed\",\"workout_session_ids\":[\"" + s2 + "\",\"" + s1 + "\"]},"
                + "{\"day_of_week\":2,\"day_type\":\"rest\",\"workout_session_ids\":[]}]},{\"days\":[]}]}";

        final UUID id = save(coach, "", payload, "active");

        assertThat(jdbc.queryForObject("SELECT status || '|' || duration_weeks FROM programs WHERE id = ?", String.class, id))
                .isEqualTo("active|2");
        assertThat(jdbc.queryForList("SELECT s.workout_session_id FROM program_day_sessions s JOIN program_days d ON d.id = s.program_day_id"
                + " JOIN program_weeks w ON w.id = d.program_week_id WHERE w.program_id = ? ORDER BY s.position", UUID.class, id))
                .containsExactly(s2, s1);
        assertThat(jdbc.queryForObject("SELECT d.workout_session_id FROM program_days d JOIN program_weeks w ON w.id = d.program_week_id"
                + " WHERE w.program_id = ? AND d.day_of_week = 1", UUID.class, id)).isEqualTo(s2);
    }

    @Test
    void titleIsRequired() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/programs/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("payload", "{\"title\":\"\",\"weeks\":[]}"))
                .andExpect(header().string("HX-Trigger", containsString("Titre requis")));
    }

    @Test
    void coachCannotAssignAProgramToAMemberOfAnotherCoach() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);

        mvc.perform(post("/app/coach/programs/save").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("payload", "{\"title\":\"P\",\"assignee_id\":\"" + other.id() + "\",\"weeks\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateIsADraftWithoutAssigneeAndSettersCannotDuplicate() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final UUID id = save(owner, "", "{\"title\":\"Source\",\"assignee_id\":\"" + member.id() + "\",\"start_date\":\"2026-11-02\","
                + "\"weeks\":[{\"days\":[{\"day_of_week\":3,\"day_type\":\"review\"}]}]}", "active");

        final MvcResult copy = mvc.perform(post("/app/coach/programs/{id}/duplicate", id).header("HX-Request", "true")
                .with(owner.login()).with(csrf())).andReturn();
        final UUID copyId = UUID.fromString(copy.getResponse().getHeader("HX-Redirect").substring("/app/coach/programs/".length()));
        assertThat(jdbc.queryForObject("SELECT title || '|' || status || '|' || (assignee_id IS NULL) || '|' || (start_date IS NULL)"
                + " FROM programs WHERE id = ?", String.class, copyId)).isEqualTo("Source (copie)|draft|true|true");

        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);
        mvc.perform(post("/app/coach/programs/{id}/duplicate", id).header("HX-Request", "true").with(setter.login()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private UUID save(final TestUsers.Account user, final String id, final String payload, final String status) throws Exception {
        final MvcResult result = mvc.perform(post("/app/coach/programs/save").header("HX-Request", "true").with(user.login()).with(csrf())
                .param("id", id).param("payload", payload).param("status", status)).andReturn();
        return UUID.fromString(result.getResponse().getHeader("HX-Redirect").substring("/app/coach/programs/".length()));
    }

    private UUID session(final String title) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workout_sessions (id, title) VALUES (?, ?)", id, title);
        return id;
    }
}
