package fr.mtpilates.nutrition;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lectures simples sur les plans nutrition, utiles hors du module nutrition (tableau de bord membre). */
@Component
public class NutritionPlanQueries {

    private final JdbcTemplate jdbc;

    public NutritionPlanQueries(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** hasActivePlan de l'original : au moins un plan actif pour ce membre. */
    public boolean hasActivePlan(final UUID memberId) {
        final Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM nutrition_plans WHERE member_id = ? AND is_active = true", Integer.class, memberId);
        return count != null && count > 0;
    }
}
