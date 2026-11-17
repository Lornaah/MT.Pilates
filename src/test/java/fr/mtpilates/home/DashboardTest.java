package fr.mtpilates.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class DashboardTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void staffSeesStatsAndUpcomingBirthdaysOfTheirMembers() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("anniv", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ?, date_of_birth = ?, phone = '06 11 22 33 44' WHERE id = ?",
                coach.id(), LocalDate.now().plusDays(2).minusYears(30), member.id());

        mvc.perform(get("/app").with(coach.login()))
                .andExpect(content().string(containsString("Bonjour Coach")))
                .andExpect(content().string(containsString("Membres actifs")))
                .andExpect(content().string(containsString("Anniv Test")))
                .andExpect(content().string(containsString("https://wa.me/0611223344?text=Joyeux%20anniversaire")));
    }

    @Test
    void memberSeesTheirWeekAndQuickLinks() throws Exception {
        final TestUsers.Account member = users.create("lea", AppRole.MEMBER);
        jdbc.update("INSERT INTO events (id, title, type, starts_at, ends_at, member_id) VALUES (?, 'Mon suivi perso', 'one_on_one', now(), now() + interval '1 hour', ?)",
                UUID.randomUUID(), member.id());

        mvc.perform(get("/app").with(member.login()))
                .andExpect(content().string(containsString("Bonjour, ")))
                .andExpect(content().string(containsString("Mon suivi perso")))
                .andExpect(content().string(containsString("En attente de ton coach")));
    }

    @Test
    void whatsappLinkKeepsOnlyDigits() {
        assertThat(DashboardService.whatsapp(Optional.of("+33 6 11"), "Salut à toi"))
                .contains("https://wa.me/33611?text=Salut%20%C3%A0%20toi");
        assertThat(DashboardService.whatsapp(Optional.of("---"), "x")).isEmpty();
    }
}
