package fr.mtpilates.prospect;

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
class ProspectControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void setterCreatesALeadWithParisAppointment() throws Exception {
        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);
        final String name = "Lead " + setter.id().toString().substring(0, 8);

        mvc.perform(post("/app/prospects").header("HX-Request", "true").with(setter.login()).with(csrf())
                        .param("firstName", " " + name + " ").param("phone", "+33 6 12 34 56 78").param("age", "34")
                        .param("source", "tiktok").param("status", "to_call").param("appointmentAt", "2026-07-01T14:30"))
                .andExpect(header().string("HX-Trigger", containsString("Prospect ajout\\u00E9")))
                .andExpect(content().string(containsString(name)))
                .andExpect(content().string(containsString("https://wa.me/33612345678?text=Bonjour%20" + name.replace(" ", "%20"))));
        assertThat(jdbc.queryForObject("SELECT appointment_at::text || '|' || source || '|' || created_by FROM prospects WHERE first_name = ?",
                String.class, name)).isEqualTo("2026-07-01 12:30:00+00|tiktok|" + setter.id());
    }

    @Test
    void firstNameIsRequired() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);

        mvc.perform(post("/app/prospects").header("HX-Request", "true").with(owner.login()).with(csrf()).param("firstName", " "))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Pr\\u00E9nom requis")));
    }

    @Test
    void statusChangeAndDeletionUpdateThePipeline() throws Exception {
        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);
        final UUID id = insert("Statut " + setter.id(), "lead-" + setter.id() + "@example.test");

        mvc.perform(post("/app/prospects/{id}/status", id).param("status", "trial").header("HX-Request", "true")
                        .with(setter.login()).with(csrf()))
                .andExpect(content().string(containsString("prospects-content")));
        assertThat(jdbc.queryForObject("SELECT status FROM prospects WHERE id = ?", String.class, id)).isEqualTo("trial");

        mvc.perform(delete("/app/prospects/{id}", id).header("HX-Request", "true").with(setter.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Prospect supprim\\u00E9")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM prospects WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void conversionCreatesTheMemberAccountInOneStep() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String email = "converti-" + owner.id() + "@example.test";
        final UUID id = insert("Converti", email);

        mvc.perform(post("/app/prospects/{id}/convert", id).param("coachId", coach.id().toString()).param("password", "court")
                        .header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Mot de passe : 8 caract\\u00E8res minimum")));
        mvc.perform(post("/app/prospects/{id}/convert", id).param("coachId", coach.id().toString()).param("password", "motdepasse")
                        .param("from", "calls").header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Prospect converti en membre !")))
                .andExpect(content().string(containsString("calls-content")));

        final UUID member = jdbc.queryForObject("SELECT converted_member_id FROM prospects WHERE id = ? AND status = 'converted'",
                UUID.class, id);
        assertThat(jdbc.queryForObject("SELECT p.first_name || '|' || p.phone || '|' || p.coach_id || '|' || p.status || '|' || r.role "
                + "FROM profiles p JOIN user_roles r ON r.user_id = p.id WHERE p.id = ?", String.class, member))
                .isEqualTo("Converti|0600000000|" + coach.id() + "|active|member");

        final UUID twin = insert("Doublon", email);
        mvc.perform(post("/app/prospects/{id}/convert", twin).param("coachId", "none").param("password", "motdepasse")
                        .header("HX-Request", "true").with(owner.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Un compte existe d\\u00E9j\\u00E0 avec cet email")));
    }

    @Test
    void coachesSeeNoLeadsAndCannotWriteThem() throws Exception {
        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String name = "Secret " + setter.id();
        insert(name, null);

        mvc.perform(get("/app/prospects").with(coach.login())).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(name))));
        mvc.perform(get("/app/calls").with(setter.login())).andExpect(content().string(containsString("Rendez-vous planifiés")));
        mvc.perform(post("/app/prospects").header("HX-Request", "true").with(coach.login()).with(csrf()).param("firstName", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyUrlAndBookingLinkStillWork() throws Exception {
        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);

        mvc.perform(get("/app/admin/prospects").with(setter.login())).andExpect(redirectedUrl("/app/prospects"));
        mvc.perform(get("/app/my-booking-link").with(setter.login()))
                .andExpect(content().string(containsString("Lien en cours de configuration")));
    }

    private UUID insert(final String firstName, final String email) {
        return jdbc.queryForObject("INSERT INTO prospects (first_name, email, phone, status) VALUES (?, ?, '0600000000', 'new') "
                + "RETURNING id", UUID.class, firstName, email);
    }
}
