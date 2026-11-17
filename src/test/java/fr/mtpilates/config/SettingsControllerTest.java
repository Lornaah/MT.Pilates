package fr.mtpilates.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SettingsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreSeededLabels() {
        jdbc.update("UPDATE app_config SET value = 'Mon calendrier' WHERE key = 'nav_calendar'");
        jdbc.update("DELETE FROM app_config WHERE key IN ('nav_habits', 'status_label_active')");
    }

    @Test
    void showsEverySectionWithTheStoredValues() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);

        mvc.perform(get("/app/owner/settings").with(owner.login()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rôles &amp; statuts")))
                .andExpect(content().string(containsString("value=\"Mon calendrier\"")))
                .andExpect(content().string(containsString("Configuration de l&#39;application")));
    }

    @Test
    void savesChangesCountsThemAndRefreshesTheSidebar() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        jdbc.update("UPDATE app_config SET value = 'Tableau de bord' WHERE key = 'nav_dashboard'");

        mvc.perform(post("/app/owner/settings").header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("cfg.nav_calendar", "Agenda")
                        .param("cfg.nav_habits", "Rituels")
                        .param("cfg.status_label_active", "")
                        .param("cfg.nav_dashboard", "Tableau de bord")
                        .param("cfg.program_objectives", "piratage"))
                .andExpect(header().string("HX-Trigger", containsString("2 changement(s)")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\"")))
                .andExpect(content().string(containsString("Agenda")));

        assertThat(value("nav_calendar")).containsExactly("Agenda");
        assertThat(value("nav_habits")).containsExactly("Rituels");
        assertThat(value("status_label_active")).isEmpty();
        assertThat(value("program_objectives")).doesNotContain("piratage");
    }

    @Test
    void resetEmptiesEveryValue() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final List<Map<String, Object>> snapshot = jdbc.queryForList("SELECT key, value FROM app_config");
        try {
            mvc.perform(post("/app/owner/settings/reset").header("HX-Request", "true").with(owner.login()).with(csrf()))
                    .andExpect(header().string("HX-Trigger", containsString("Valeurs remises aux d\\u00E9fauts")))
                    .andExpect(content().string(containsString("id=\"settings-fields\"")));

            assertThat(jdbc.queryForObject("SELECT count(*) FROM app_config WHERE value <> ''", Integer.class)).isZero();
        } finally {
            snapshot.forEach(row -> jdbc.update("UPDATE app_config SET value = ? WHERE key = ?", row.get("value"), row.get("key")));
        }
    }

    @Test
    void isReservedToTheOwner() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(get("/app/owner/settings").with(coach.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(post("/app/owner/settings/reset").with(coach.login()).with(csrf())).andExpect(redirectedUrl("/app"));
        assertThat(value("nav_calendar")).containsExactly("Mon calendrier");
    }

    private List<String> value(final String key) {
        return jdbc.queryForList("SELECT value FROM app_config WHERE key = ?", String.class, key);
    }
}
