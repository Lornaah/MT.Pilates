package fr.mtpilates.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class TrackingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void memberAddsEntriesWithTrendsAgainstThePreviousOne() throws Exception {
        final TestUsers.Account member = users.create("suivi", AppRole.MEMBER);
        addEntry(member, "2026-10-01", "64");
        mvc.perform(post("/app/tracking/entries").header("HX-Request", "true").with(member.login()).with(csrf())
                        .param("date", "2026-11-02").param("weightKg", "62.5").param("notes", "RAS"))
                .andExpect(header().string("HX-Trigger", containsString("Entr\\u00E9e ajout\\u00E9e")))
                .andExpect(content().string(containsString("lundi 2 novembre 2026")))
                .andExpect(content().string(containsString("62.5")))
                .andExpect(content().string(containsString("lucide-trending-down")));
    }

    @Test
    void onlyTheMemberDeletesTheirEntries() throws Exception {
        final TestUsers.Account member = users.create("suivi", AppRole.MEMBER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        addEntry(member, "2026-10-01", "64");
        final UUID entry = jdbc.queryForObject("SELECT id FROM member_tracking WHERE user_id = ?", UUID.class, member.id());

        mvc.perform(delete("/app/tracking/entries/{id}", entry).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/app/tracking/entries/{id}", entry).header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Supprim")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_tracking WHERE id = ?", Integer.class, entry)).isZero();
    }

    @Test
    void weeklyReviewIsReplacedForTheSameWeek() throws Exception {
        final TestUsers.Account member = users.create("bilan", AppRole.MEMBER);
        for (final String score : new String[] {"5", "9"}) {
            mvc.perform(post("/app/tracking/reviews").header("HX-Request", "true").with(member.login()).with(csrf())
                            .param("weekStart", "2026-11-09").param("score", score).param("note", "Note " + score))
                    .andExpect(header().string("HX-Trigger", containsString("Bilan enregistr\\u00E9")));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_reviews WHERE user_id = ?", Integer.class, member.id())).isOne();
        assertThat(jdbc.queryForObject("SELECT mental_wellbeing FROM weekly_reviews WHERE user_id = ?", Integer.class,
                member.id())).isEqualTo(9);
    }

    @Test
    void photosAreReadableByTheMemberAndTheirCoachOnly() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("photo", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        final TestUsers.Account stranger = users.create("autre", AppRole.MEMBER);
        final TestUsers.Account otherCoach = users.create("autrecoach", AppRole.COACH);

        mvc.perform(multipart("/app/members/{id}/photos", member.id())
                        .file(new MockMultipartFile("file", "dos.png", "image/png", new byte[] {7, 7}))
                        .param("side", "back").param("takenAt", "2026-11-01")
                        .header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Photo ajout\\u00E9e")))
                .andExpect(content().string(containsString("Dos")));

        final String url = jdbc.queryForObject("SELECT photo_url FROM member_photos WHERE user_id = ?", String.class, member.id());
        mvc.perform(get(url).with(member.login())).andExpect(status().isOk());
        mvc.perform(get(url).with(coach.login())).andExpect(status().isOk());
        mvc.perform(get(url).with(stranger.login())).andExpect(status().isNotFound());
        mvc.perform(get(url).with(otherCoach.login())).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(multipart("/app/members/{id}/photos", member.id())
                        .file(new MockMultipartFile("file", "x.png", "image/png", new byte[] {1}))
                        .header("HX-Request", "true").with(stranger.login()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentsNeedATitleAndAreListedWithTheirCategory() throws Exception {
        final TestUsers.Account member = users.create("doc", AppRole.MEMBER);
        final MockMultipartFile file = new MockMultipartFile("file", "ordo nnance.pdf", "application/pdf", new byte[2048]);

        mvc.perform(multipart("/app/members/{id}/documents", member.id()).file(file).param("title", " ")
                        .header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(header().string("HX-Reswap", "none"));
        mvc.perform(multipart("/app/members/{id}/documents", member.id()).file(file).param("title", "Ordonnance")
                        .param("category", "Médical").header("HX-Request", "true").with(member.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Document ajout\\u00E9")))
                .andExpect(content().string(containsString("Ordonnance")))
                .andExpect(content().string(containsString("2.0 Ko")));
        assertThat(jdbc.queryForObject("SELECT file_url FROM member_documents WHERE user_id = ?", String.class, member.id()))
                .endsWith("-ordo_nnance.pdf");
    }

    @Test
    void staffExportsMeasurementsAsCsvButOtherMembersCannot() throws Exception {
        final TestUsers.Account member = users.create("csv", AppRole.MEMBER);
        final TestUsers.Account setter = users.create("setter", AppRole.SETTER);
        final TestUsers.Account stranger = users.create("autre", AppRole.MEMBER);
        addEntry(member, "2026-10-01", "64");
        jdbc.update("UPDATE member_tracking SET notes = 'Dos, \"genou\"' WHERE user_id = ?", member.id());

        mvc.perform(get("/app/members/{id}/tracking.csv", member.id()).with(setter.login()))
                .andExpect(header().string("Content-Disposition", containsString("mensurations-")))
                .andExpect(content().string(containsString("2026-10-01,64,,,,,,,\"Dos, \"\"genou\"\"\"")));
        mvc.perform(get("/app/members/{id}/tracking.csv", member.id()).with(stranger.login()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void comparesTheClosestEntries() throws Exception {
        final TestUsers.Account member = users.create("compare", AppRole.MEMBER);
        addEntry(member, "2026-09-01", "65");
        addEntry(member, "2026-11-01", "62");

        mvc.perform(get("/app/members/{id}/tracking/compare", member.id()).param("a", "2026-09-05").param("b", "2026-10-30")
                        .with(member.login()))
                .andExpect(content().string(containsString("1 sept. 2026")))
                .andExpect(content().string(containsString("65 → 62")))
                .andExpect(content().string(containsString("(-3.0)")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    private void addEntry(final TestUsers.Account member, final String date, final String weight) {
        jdbc.update("INSERT INTO member_tracking (user_id, date, weight_kg) VALUES (?, ?::date, ?::numeric)",
                member.id(), date, weight);
    }
}
