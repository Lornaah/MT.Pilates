package fr.mtpilates.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

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
class CalendarControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachCreatesAPrivateOneHourEventInParisTime() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Suivi " + coach.id().toString().substring(0, 8);

        mvc.perform(post("/app/calendar/events").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", title).param("type", "call").param("startsAt", "2026-07-01T10:00").param("memberId", "none")
                        .param("myMonth", "2026-07"))
                .andExpect(header().string("HX-Trigger", containsString("\\u00C9v\\u00E9nement cr\\u00E9\\u00E9")))
                .andExpect(content().string(containsString(title)));

        final Map<String, Object> row = jdbc.queryForMap("SELECT to_char(starts_at AT TIME ZONE 'UTC', 'HH24:MI') AS start,"
                + " extract(epoch FROM ends_at - starts_at) AS secs, is_private, coach_id, type FROM events WHERE title = ?", title);
        assertThat(row).containsEntry("start", "08:00").containsEntry("is_private", true).containsEntry("coach_id", coach.id())
                .containsEntry("type", "call");
        assertThat(((Number) row.get("secs")).intValue()).isEqualTo(3600);
    }

    @Test
    void titleAndDateAreRequired() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/calendar/events").header("HX-Request", "true").with(coach.login()).with(csrf()).param("title", "Sans date"))
                .andExpect(header().string("HX-Trigger", containsString("Titre et date/heure requis")));
    }

    @Test
    void coachSeesOnlyTheirEventsAndCannotDeleteOthers() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account other = users.create("autre", AppRole.COACH);
        final UUID theirs = event(other, "Événement d'un autre coach");

        mvc.perform(get("/app/calendar").with(coach.login()))
                .andExpect(content().string(not(containsString("Événement d&#39;un autre coach"))))
                .andExpect(content().string(not(containsString("Calendrier général"))));
        mvc.perform(delete("/app/calendar/events/{id}", theirs).header("HX-Request", "true").with(coach.login()).with(csrf()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM events WHERE id = ?", Integer.class, theirs)).isOne();
    }

    @Test
    void ownerSeesTheGeneralCalendar() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        event(coach, "Cours du coach");

        mvc.perform(get("/app/calendar").with(owner.login()))
                .andExpect(content().string(containsString("Calendrier général")))
                .andExpect(content().string(containsString("Cours du coach")));
    }

    private UUID event(final TestUsers.Account coach, final String title) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO events (id, title, type, starts_at, ends_at, coach_id, created_by) VALUES (?, ?, 'call', now(), now() + interval '1 hour', ?, ?)",
                id, title, coach.id(), coach.id());
        return id;
    }
}
