package fr.mtpilates.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class PlanningControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void memberSeesTheActiveProgramAndMarksTodayDone() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final UUID program = UUID.randomUUID();
        final UUID week = UUID.randomUUID();
        final UUID day = UUID.randomUUID();
        jdbc.update("INSERT INTO programs (id, title, status, assignee_id, start_date) VALUES (?, 'Programme du jour', 'active', ?, ?)",
                program, member.id(), LocalDate.now().minusDays(1));
        jdbc.update("INSERT INTO program_weeks (id, program_id, week_number, week_comment) VALUES (?, ?, 1, 'Courage')", week, program);
        jdbc.update("INSERT INTO program_days (id, program_week_id, day_of_week, day_type) VALUES (?, ?, ?, 'review')",
                day, week, LocalDate.now().getDayOfWeek().getValue());

        mvc.perform(get("/app/planning").with(member.login()))
                .andExpect(content().string(containsString("Programme du jour")))
                .andExpect(content().string(containsString("Bilan hebdo")))
                .andExpect(content().string(containsString("Note de la semaine : Courage")));

        mvc.perform(post("/app/planning/days/{id}/toggle", day).header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(content().string(containsString("Fait ✓")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM program_day_completions WHERE program_day_id = ?", Integer.class, day)).isOne();
        mvc.perform(post("/app/planning/days/{id}/toggle", day).header("HX-Request", "true").with(member.login()).with(csrf()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM program_day_completions WHERE program_day_id = ?", Integer.class, day)).isZero();
    }

    @Test
    void anotherMembersProgramIsNotShown() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);
        jdbc.update("INSERT INTO programs (id, title, status, assignee_id) VALUES (?, 'Pas pour toi', 'active', ?)", UUID.randomUUID(), other.id());

        mvc.perform(get("/app/planning").with(member.login()))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Pas pour toi"))));
    }
}
