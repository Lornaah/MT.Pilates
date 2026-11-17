package fr.mtpilates.document;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.mtpilates.support.IntegrationTest;
import fr.mtpilates.support.TestUsers;
import fr.mtpilates.user.AppRole;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class DocumentControllerTest {

    private static final Pattern FILE_URL = Pattern.compile("name=\"fileUrl\" value=\"([^\"]+)\"");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void coachUploadsAndSharesADocumentWithOneMember() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account allowed = users.create("membre", AppRole.MEMBER);
        final TestUsers.Account other = users.create("autre", AppRole.MEMBER);
        final String path = upload(coach, "Contrat.pdf");
        final String title = "Contrat " + allowed.id().toString().substring(0, 8);

        mvc.perform(post("/app/coach/documents").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", title).param("category", "contrat").param("visibility", "specific")
                        .param("sharedWithIds", allowed.id().toString()).param("tags", "admin")
                        .param("fileUrl", path).param("fileSize", "7").param("mimeType", "application/pdf"))
                .andExpect(header().string("HX-Trigger", containsString("Document ajout\\u00E9")))
                .andExpect(content().string(containsString(title)));
        final UUID id = jdbc.queryForObject("SELECT id FROM documents WHERE title = ?", UUID.class, title);

        mvc.perform(get("/app/documents/{id}", id).with(allowed.login()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes("contenu".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/app/documents/{id}", id).with(other.login())).andExpect(status().isNotFound());
        mvc.perform(get("/app/profile").with(allowed.login())).andExpect(content().string(containsString(title)));
        mvc.perform(get("/app/profile").with(other.login())).andExpect(content().string(not(containsString(title))));
    }

    @Test
    void uploadSuggestsTheFileNameAsTitle() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(multipart("/app/coach/documents/upload").file(file("Recettes healthy.pdf"))
                        .header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(content().string(containsString("value=\"Recettes healthy\"")))
                .andExpect(content().string(containsString("Fichier prêt")));
    }

    @Test
    void savingWithoutFileIsRefused() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/documents").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("title", "Sans fichier"))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Titre et fichier requis")));
    }

    @Test
    void staffOnlyDocumentsStayHiddenFromMembers() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final String path = upload(coach, "interne.pdf");
        final UUID id = insert("Interne " + member.id(), "staff_only", path);

        mvc.perform(get("/app/documents/{id}", id).with(member.login())).andExpect(status().isNotFound());
        mvc.perform(get("/app/documents/{id}", id).with(coach.login())).andExpect(status().isOk());
    }

    @Test
    void deletingADocumentAlsoDeletesItsFile() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String path = upload(coach, "a-supprimer.pdf");
        final UUID id = insert("Supprimé " + coach.id(), "all_members", path);

        mvc.perform(delete("/app/coach/documents/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Document supprim\\u00E9")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM documents WHERE id = ?", Integer.class, id)).isZero();
        final TestUsers.Account other = users.create("coach", AppRole.COACH);
        final UUID copy = insert("Même fichier " + coach.id(), "all_members", path);
        mvc.perform(get("/app/documents/{id}", copy).with(other.login())).andExpect(status().isNotFound());
    }

    @Test
    void membersCannotManageTheLibrary() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/documents").with(member.login())).andExpect(redirectedUrl("/app"));
        mvc.perform(post("/app/coach/documents").header("HX-Request", "true").with(member.login()).with(csrf())
                        .param("title", "Intrus").param("fileUrl", "x.pdf"))
                .andExpect(status().isForbidden());
    }

    private String upload(final TestUsers.Account account, final String filename) throws Exception {
        final String html = mvc.perform(multipart("/app/coach/documents/upload").file(file(filename))
                        .header("HX-Request", "true").with(account.login()).with(csrf()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        final Matcher matcher = FILE_URL.matcher(html);
        assertThat(matcher.find()).as("chemin du fichier téléversé").isTrue();
        return matcher.group(1);
    }

    private static MockMultipartFile file(final String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", "contenu".getBytes(StandardCharsets.UTF_8));
    }

    private UUID insert(final String title, final String visibility, final String path) {
        return jdbc.queryForObject("INSERT INTO documents (title, category, visibility, file_url) VALUES (?, 'guide', ?, ?) RETURNING id",
                UUID.class, title, visibility, path);
    }
}
