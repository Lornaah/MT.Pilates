package fr.mtpilates.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class FinanceControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void filtersSortAndSyncTheSourceFromTheConvertedLead() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);
        final String tag = member.id().toString().substring(0, 8);
        jdbc.update("UPDATE profiles SET first_name = ?, contract_amount = 1234.5, paid_amount = 200 WHERE id = ?", "Zoé" + tag, member.id());
        jdbc.update("INSERT INTO prospects (first_name, source, status, converted_member_id) VALUES ('Zoé', 'word_of_mouth', 'converted', ?)",
                member.id());

        mvc.perform(get("/app/money/content").param("q", "zoé" + tag).header("HX-Request", "true").with(owner.login()))
                .andExpect(content().string(containsString("Word Of Mouth")))
                .andExpect(content().string(containsString("1 234,50 €")))
                .andExpect(content().string(containsString("1 034,50 € à encaisser")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\"")));

        final String csv = mvc.perform(get("/app/money/export").param("q", "zoé" + tag).with(owner.login()))
                .andExpect(header().string("Content-Disposition", containsString("finance-")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Nom;Prénom;").contains(";Zoé" + tag + ";").contains(";Word Of Mouth;1234.50;200.00;1034.50;");
    }

    @Test
    void ownerEditsBillingAndCoachesCannotEditOthers() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account coach = users.create("coach", AppRole.COACH);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(post("/app/money/{id}", member.id()).header("HX-Request", "true").with(owner.login()).with(csrf())
                        .param("source", "Parrainage").param("contracted", "600").param("paid", "150.5").param("installments", "3")
                        .param("notes", "Relance le 10"))
                .andExpect(header().string("HX-Trigger", containsString("Fiche mise \\u00E0 jour")));
        assertThat(jdbc.queryForObject("SELECT billing_source || '|' || contract_amount || '|' || paid_amount || '|' || payment_installments "
                + "FROM profiles WHERE id = ?", String.class, member.id())).isEqualTo("Parrainage|600.00|150.50|3");

        mvc.perform(post("/app/money/{id}", member.id()).header("HX-Request", "true").with(coach.login()).with(csrf())
                        .param("contracted", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyUrlRedirectsAndMembersAreKeptOut() throws Exception {
        final TestUsers.Account owner = users.create("owner", AppRole.OWNER);
        final TestUsers.Account member = users.create("membre", AppRole.MEMBER);

        mvc.perform(get("/app/admin/finance").with(owner.login())).andExpect(redirectedUrl("/app/money"));
        mvc.perform(get("/app/money").with(owner.login())).andExpect(content().string(containsString("Évolution du CA (12 mois)")));
        mvc.perform(get("/app/money").with(member.login())).andExpect(redirectedUrl("/app"));
    }

    @Test
    void helpersFollowTheOriginal() {
        assertThat(FinanceService.prettySource("word_of_mouth")).contains("Word Of Mouth");
        assertThat(FinanceService.prettySource("Bouche-à-oreille")).contains("Bouche-à-Oreille");
        assertThat(FinanceService.prettySource("")).isEqualTo(Optional.empty());
        assertThat(FinanceService.sanitizePhone("06 11.22-33 (44)")).isEqualTo("33611223344");
        assertThat(FinanceService.sanitizePhone("0033 6 11")).isEqualTo("33611");
        assertThat(FinanceService.eur(new BigDecimal("-12.5"))).isEqualTo("-12,50 €");
    }
}
