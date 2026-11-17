package fr.mtpilates.habit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class HabitControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachCreatesAHabitWithAWhitelistedIcon() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Dormir " + coach.id().toString().substring(0, 8);

        mvc.perform(post("/app/coach/habits").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", title).param("icon", "inconnue").param("frequency", "weekly").param("global", "false", "true"))
                .andExpect(header().string("HX-Trigger", containsString("Habitude cr\\u00E9\\u00E9e")))
                .andExpect(content().string(containsString(title)));
        assertThat(jdbc.queryForObject("SELECT icon || '|' || frequency || '|' || is_global FROM habits WHERE title = ?", String.class, title))
                .isEqualTo("sparkles|weekly|true");
    }

    @Test
    void assignmentsAreReplacedByDifferenceForCoachedMembersOnly() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account mine = coached(coach);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);
        final UUID habit = habit("Marcher", false);

        mvc.perform(post("/app/coach/habits/{id}/assign", habit).header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("userIds", mine.id().toString()))
                .andExpect(header().string("HX-Trigger", containsString("Assignations mises \\u00E0 jour")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM habit_assignments WHERE habit_id = ?", Integer.class, habit)).isOne();

        mvc.perform(post("/app/coach/habits/{id}/assign", habit).header("HX-Request", "true").with(coach.login()).with(csrf()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM habit_assignments WHERE habit_id = ?", Integer.class, habit)).isZero();

        mvc.perform(post("/app/coach/habits/{id}/assign", habit).header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("userIds", other.id().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberTogglesTodayAndSeesTheStreak() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final UUID habit = habit("Eau " + member.id().toString().substring(0, 8), false);
        jdbc.update("INSERT INTO habit_assignments (habit_id, user_id) VALUES (?, ?)", habit, member.id());
        jdbc.update("INSERT INTO habit_completions (habit_id, user_id, completed_date) VALUES (?, ?, ?)", habit, member.id(),
                LocalDate.now().minusDays(1));

        mvc.perform(post("/app/habits/{id}/toggle", habit).header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(content().string(containsString("Mes habitudes du jour")))
                .andExpect(content().string(containsString("line-through")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM habit_completions WHERE habit_id = ?", Integer.class, habit)).isEqualTo(2);
        assertThat(HabitService.streak(java.util.Set.of(LocalDate.now(), LocalDate.now().minusDays(1)), LocalDate.now())).isEqualTo(2);
    }

    @Test
    void coachSeesTheTrackingOfTheirMemberOverTheChosenRange() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = coached(coach);
        final UUID habit = habit("Suivi " + member.id().toString().substring(0, 8), false);
        jdbc.update("INSERT INTO habit_assignments (habit_id, user_id) VALUES (?, ?)", habit, member.id());
        jdbc.update("INSERT INTO habit_completions (habit_id, user_id, completed_date) VALUES (?, ?, ?)", habit, member.id(), LocalDate.now());

        mvc.perform(get("/app/members/{id}/habits", member.id()).param("range", "7").with(coach.login()))
                .andExpect(content().string(containsString("1/7j")))
                .andExpect(content().string(containsString("14%")));
    }

    private UUID habit(final String title, final boolean global) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO habits (id, title, is_global) VALUES (?, ?, ?)", id, title, global);
        return id;
    }

    private TestUsers.Account coached(final TestUsers.Account coach) {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        return member;
    }
}
