package fr.mtpilates.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import fr.mtpilates.shared.ui.RelativeTime;
import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachPublishesARecurringAnnouncementTargetedToTheirMembers() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Rappel " + coach.id().toString().substring(0, 8);

        mvc.perform(post("/app/coach/announcements").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", title).param("body", "**Gras** <script>alert(1)</script>").param("target", "by_coach")
                        .param("targetCoachId", coach.id().toString()).param("recurring", "false", "true")
                        .param("recurrenceFreq", "WEEKLY").param("recurrenceDays", "MO", "TH").param("publishAt", "2026-01-05T09:00"))
                .andExpect(header().string("HX-Trigger", containsString("Annonce enregistr\\u00E9e")))
                .andExpect(content().string(containsString("<strong>Gras</strong>")))
                .andExpect(content().string(not(containsString("<script>"))));
        assertThat(jdbc.queryForObject("SELECT recurrence_rule || '|' || target_coach_id FROM announcements WHERE title = ?", String.class, title))
                .isEqualTo("FREQ=WEEKLY;BYDAY=MO,TH|" + coach.id());
    }

    @Test
    void membersSeeOnlyPublishedAnnouncementsTargetedToThem() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        final String tag = member.id().toString().substring(0, 8);
        insert("Pour ses membres " + tag, "by_coach", coach.id(), List.of(), "2026-01-01T00:00:00Z");
        insert("Pour une autre " + tag, "specific", null, List.of(UUID.randomUUID()), "2026-01-01T00:00:00Z");
        insert("Future " + tag, "all", null, List.of(), "2098-01-01T00:00:00Z");

        mvc.perform(get("/app/announcements").with(member.login()))
                .andExpect(content().string(containsString("Pour ses membres " + tag)))
                .andExpect(content().string(not(containsString("Pour une autre " + tag))))
                .andExpect(content().string(not(containsString("Future " + tag))));
    }

    @Test
    void markingAsReadUpdatesTheBell() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final UUID id = insert("Lue " + member.id(), "specific", null, List.of(member.id()), "2026-01-01T00:00:00Z");

        mvc.perform(post("/app/announcements/{id}/read", id).param("from", "bell").header("HX-Request", "true")
                        .with(member.login()).with(csrf()))
                .andExpect(content().string(containsString("notification-bell")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM announcement_reads WHERE announcement_id = ? AND user_id = ?",
                Integer.class, id, member.id())).isOne();
    }

    @Test
    void membersCannotManageAnnouncements() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/announcements").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void relativeTimeFollowsDateFnsWording() {
        final Instant now = Instant.parse("2026-11-17T12:00:00Z");
        assertThat(RelativeTime.between(now.minusSeconds(3 * 86_400), now)).isEqualTo("il y a 3 jours");
        assertThat(RelativeTime.between(now.minusSeconds(2 * 3600), now)).isEqualTo("il y a environ 2 heures");
        assertThat(RelativeTime.between(now.plusSeconds(600), now)).isEqualTo("dans 10 minutes");
    }

    private UUID insert(final String title, final String target, final UUID coach, final List<UUID> users, final String publishAt) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO announcements (id, title, body, target, target_coach_id, target_user_ids, publish_at) VALUES (?, ?, 'Corps', ?, ?, ?::uuid[], ?::timestamptz)",
                id, title, target, coach, "{" + String.join(",", users.stream().map(UUID::toString).toList()) + "}", publishAt);
        return id;
    }
}
