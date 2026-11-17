package fr.mtpilates.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class NutritionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private OpenFoodFactsClient openFoodFacts;

    @Test
    void everyTabRendersForStaff() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        for (final String tab : List.of("overview", "recipes", "foods", "templates")) {
            mvc.perform(get("/app/coach/nutrition").param("tab", tab).with(coach.login())).andExpect(status().isOk());
        }
        mvc.perform(get("/app/coach/nutrition").with(coach.login()))
                .andExpect(content().string(containsString("Trois façons de créer un plan")));
    }

    @Test
    void legacyUrlsRedirectToTheHubTabs() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(get("/app/coach/recipes").with(coach.login())).andExpect(redirectedUrl("/app/coach/nutrition?tab=recipes"));
        mvc.perform(get("/app/coach/foods").with(coach.login())).andExpect(redirectedUrl("/app/coach/nutrition?tab=foods"));
    }

    @Test
    void membersCannotOpenTheHub() throws Exception {
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/coach/nutrition").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void coachCreatesAFoodAndNameIsRequired() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String name = "Lentilles " + coach.id().toString().substring(0, 8);

        mvc.perform(post("/app/coach/nutrition/foods").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("name", "  " + name + " ").param("calories", "116").param("protein", "9,0")
                        .param("source", "ciqual").param("aisle", "epicerie_salee").param("isPublic", "true"))
                .andExpect(header().string("HX-Trigger", containsString("Aliment ajout\\u00E9")))
                .andExpect(content().string(containsString(name)));
        assertThat(jdbc.queryForObject("SELECT protein_g_per_100g || '|' || source || '|' || is_public FROM foods WHERE name = ?",
                String.class, name)).isEqualTo("9.0|ciqual|true");

        mvc.perform(post("/app/coach/nutrition/foods").header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("name", " "))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Nom requis")));
    }

    @Test
    void barcodeLookupPrefillsTheFoodDialog() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        when(openFoodFacts.byBarcode("3017620422003")).thenReturn(Optional.of(new OffProduct(Optional.empty(),
                "Nutella", Optional.of("Ferrero"), Optional.empty(), Optional.of(new BigDecimal("15")),
                Optional.of(new BigDecimal("539.0")), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        when(openFoodFacts.byBarcode("000")).thenReturn(Optional.empty());

        mvc.perform(post("/app/coach/nutrition/foods/barcode").param("barcode", " 3017620422003 ").header("HX-Request", "true")
                        .with(coach.login()).with(csrf()))
                .andExpect(content().string(containsString("value=\"Nutella\"")))
                .andExpect(content().string(containsString("value=\"3017620422003\"")))
                .andExpect(content().string(containsString("value=\"539\"")))
                .andExpect(content().string(containsString("<option value=\"open_food_facts\" selected=\"selected\">")));
        mvc.perform(post("/app/coach/nutrition/foods/barcode").param("barcode", "000").header("HX-Request", "true")
                        .with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Reswap", "none"))
                .andExpect(header().string("HX-Trigger", containsString("Produit introuvable sur OpenFoodFacts")));
    }

    @Test
    void searchNeedsTwoCharacters() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);

        mvc.perform(post("/app/coach/nutrition/foods/search").param("q", "a").header("HX-Request", "true")
                        .with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Saisis au moins 2 caract\\u00E8res")));
    }

    @Test
    void coachSavesARecipeThatBecomesTheirs() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Soupe " + coach.id().toString().substring(0, 8);
        final String payload = """
                {"title":"%s","category":"lunch_dinner","calories":"250","protein_g":"12.5","servings":"2",
                 "ingredients":[{"name":"Carotte, bio","quantity":"3","unit":"pièce(s)"},{"name":"  ","quantity":"","unit":"g"}],
                 "dietary_tags":["vegan","inconnu"],"allergens":[],"equipment_needed":["stovetop"],"meal_types":["dinner"]}
                """.formatted(title);

        mvc.perform(post("/app/coach/nutrition/recipes").param("payload", payload).header("HX-Request", "true")
                        .with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Recette cr\\u00E9\\u00E9e")))
                .andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString("id=\"recipe-count-mine\"")));
        assertThat(jdbc.queryForObject("SELECT jsonb_array_length(ingredients) || '|' || array_to_string(dietary_tags, ',') "
                + "|| '|' || created_by FROM recipes WHERE title = ?", String.class, title)).isEqualTo("1|vegan|" + coach.id());

        mvc.perform(post("/app/coach/nutrition/recipes").param("payload", "{\"title\":\" \"}").header("HX-Request", "true")
                        .with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Le titre est requis")));
    }

    @Test
    void assigningATemplateCopiesTheChosenWeekAndCountsTheUse() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        jdbc.update("UPDATE profiles SET coach_id = ? WHERE id = ?", coach.id(), member.id());
        final UUID template = template("Sèche " + member.id());

        mvc.perform(post("/app/coach/nutrition/templates/{id}/assign", template).param("memberId", member.id().toString())
                        .param("weekIndex", "1").header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Plan assign\\u00E9 \\u00E0 l'\\u00E9l\\u00E8ve")));

        assertThat(jdbc.queryForObject("SELECT meals->'tuesday'->0->>'recipe_title' || '|' || coach_id || '|' || kcal_target "
                + "FROM nutrition_plans WHERE member_id = ?", String.class, member.id())).isEqualTo("Bowl|" + coach.id() + "|1600");
        assertThat(jdbc.queryForObject("SELECT usage_count FROM meal_plan_templates WHERE id = ?", Integer.class, template)).isOne();
        mvc.perform(get("/app/coach/nutrition").with(coach.login()))
                .andExpect(content().string(containsString("Sèche " + member.id())));
    }

    @Test
    void coachCannotAssignToAMemberTheyDoNotManage() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final UUID template = template("Interdit " + member.id());

        mvc.perform(post("/app/coach/nutrition/templates/{id}/assign", template).param("memberId", member.id().toString())
                        .header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM nutrition_plans WHERE member_id = ?", Integer.class, member.id()))
                .isZero();
    }

    @Test
    void deletingAFoodRemovesIt() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID id = jdbc.queryForObject("INSERT INTO foods (name) VALUES ('À supprimer') RETURNING id", UUID.class);

        mvc.perform(delete("/app/coach/nutrition/foods/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Aliment supprim\\u00E9")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foods WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    void creatingATemplateRedirectsToItsEditorAndKeepsTheWeeks() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final String title = "Menu " + coach.id();
        final String payload = """
                {"title":"%s","cover_color":"coral","kcal_target":1800,"macros_target":{"protein_g":120},"meals_per_day":4,
                 "dietary_preferences":{"regime":"vegan"},"weeks":[{"week_number":1,"week_comment":"Go","days":{"monday":[]}},
                 {"week_number":2,"week_comment":null,"days":{}}]}
                """.formatted(title);

        final String location = mvc.perform(post("/app/coach/nutrition/templates/save").param("payload", payload)
                        .param("status", "published").header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader("HX-Redirect");
        final UUID id = jdbc.queryForObject("SELECT id FROM meal_plan_templates WHERE title = ?", UUID.class, title);
        assertThat(location).isEqualTo("/app/coach/nutrition/templates/" + id);
        assertThat(jdbc.queryForObject("SELECT status || '|' || duration_weeks || '|' || (weeks->0->>'week_comment') || '|' "
                + "|| (dietary_preferences->>'regime') || '|' || created_by FROM meal_plan_templates WHERE id = ?", String.class, id))
                .isEqualTo("published|2|Go|vegan|" + coach.id());

        mvc.perform(get(location).with(coach.login())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Couleur de cover")));
        mvc.perform(post("/app/coach/nutrition/templates/save").param("id", id.toString()).param("payload", payload.replace("Go", "Bis"))
                        .param("status", "draft").header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Menu type mis \\u00E0 jour")));
        assertThat(jdbc.queryForObject("SELECT status FROM meal_plan_templates WHERE id = ?", String.class, id)).isEqualTo("draft");
    }

    @Test
    void templateTitleIsRequiredAndDeletionReturnsToTheLibrary() throws Exception {
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final UUID id = template("Supprimé " + coach.id());

        mvc.perform(post("/app/coach/nutrition/templates/save").param("payload", "{\"title\":\" \",\"weeks\":[{}]}")
                        .header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Trigger", containsString("Titre requis")));
        mvc.perform(delete("/app/coach/nutrition/templates/{id}", id).header("HX-Request", "true").with(coach.login()).with(csrf()))
                .andExpect(header().string("HX-Redirect", "/app/coach/nutrition?tab=templates"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM meal_plan_templates WHERE id = ?", Integer.class, id)).isZero();
    }

    private UUID template(final String title) {
        final String week = "{\"week_number\":%d,\"week_comment\":null,\"days\":{\"tuesday\":[{\"recipe_title\":\"%s\"}]}}";
        return jdbc.queryForObject("INSERT INTO meal_plan_templates (title, status, kcal_target, duration_weeks, weeks) "
                + "VALUES (?, 'published', 1600, 2, ?::jsonb) RETURNING id", UUID.class, title,
                "[" + week.formatted(1, "Porridge") + "," + week.formatted(2, "Bowl") + "]");
    }
}
