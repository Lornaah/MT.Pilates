package fr.mtpilates.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class OnboardingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void memberWithoutOnboardingIsSentToOnboardingFromAnyAppPage() throws Exception {
        final TestUsers.Account member = newMember();

        mvc.perform(get("/app/profile").with(member.login()))
                .andExpect(redirectedUrl("/onboarding"));
        mvc.perform(get("/onboarding").with(member.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bienvenue")));
    }

    @Test
    void staffIsNeverSentToOnboarding() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        jdbc.update("UPDATE profiles SET onboarding_completed_at = NULL WHERE id = ?", coach.id());

        mvc.perform(get("/app").with(coach.login())).andExpect(status().isOk());
        mvc.perform(get("/onboarding").with(coach.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void completingOnboardingSavesTheProfileAndOpensTheApp() throws Exception {
        final TestUsers.Account member = newMember();

        mvc.perform(post("/onboarding").with(member.login()).with(csrf())
                        .param("firstName", "Chloé").param("lastName", "Petit")
                        .param("heightCm", "165").param("weeklyTargetSessions", "3")
                        .param("equipment", "Tapis").param("slots", "monday:morning")
                        .param("dietType", "Végétarien").param("likes", "Avocat").param("mealsPerDay", "4")
                        .param("externalSummary", "Pas de lactose le soir"))
                .andExpect(redirectedUrl("/app"))
                .andExpect(flash().attributeExists("toasts"));

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT first_name, height_cm, weekly_target_sessions, available_equipment::text AS equipment,"
                        + " availability::text AS availability, dietary_preferences::text AS diet, external_summary,"
                        + " onboarding_completed_at, terms_accepted_at FROM profiles WHERE id = ?", member.id());
        assertThat(row.get("first_name")).isEqualTo("Chloé");
        assertThat((java.math.BigDecimal) row.get("height_cm")).isEqualByComparingTo("165");
        assertThat(row.get("weekly_target_sessions")).isEqualTo(3);
        assertThat(row.get("equipment").toString()).contains("Tapis");
        assertThat(row.get("availability").toString()).contains("\"monday\": [\"morning\"]");
        assertThat(row.get("diet").toString()).contains("\"diet_type\": \"Végétarien\"", "\"meals_per_day\": 4", "Avocat");
        assertThat(row.get("external_summary")).isEqualTo("Pas de lactose le soir");
        assertThat(row.get("onboarding_completed_at")).isNotNull();
        assertThat(row.get("terms_accepted_at")).isNotNull();

        mvc.perform(get("/onboarding").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    private TestUsers.Account newMember() {
        final TestUsers.Account member = users.create("nouvelle", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET onboarding_completed_at = NULL WHERE id = ?", member.id());
        return member;
    }
}
