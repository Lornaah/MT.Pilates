package fr.mtpilates.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.mtpilates.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class BaselineMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsEveryTableOfTheOriginalSchema() {
        final List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tables).contains(
                "users", "profiles", "user_roles", "app_config", "videos", "video_views", "events",
                "member_tracking", "member_photos", "member_documents", "member_reports", "weekly_reviews",
                "habits", "habit_assignments", "habit_completions", "announcements", "announcement_reads",
                "documents", "exercises", "workout_sessions", "workout_session_blocks", "workout_session_exercises",
                "meal_plan_templates", "nutrition_plans", "recipes", "foods", "programs", "program_weeks",
                "program_days", "program_day_sessions", "program_day_completions", "prospects");
    }

    @Test
    void seedsTheOriginalConfigurationLabels() {
        final String calendarLabel = jdbc.queryForObject(
                "SELECT value FROM app_config WHERE key = 'nav_calendar'", String.class);
        final String objectives = jdbc.queryForObject(
                "SELECT value FROM app_config WHERE key = 'program_objectives'", String.class);

        assertThat(calendarLabel).isEqualTo("Mon calendrier");
        assertThat(objectives).startsWith("Endométriose,Règles douloureuses");
    }

    @Test
    void enforcesTheOriginalCheckConstraints() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO prospects (first_name, status) VALUES ('Léa', 'unknown')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void treatsEmailsAsCaseInsensitivelyUnique() {
        jdbc.update("INSERT INTO users (email, password_hash) VALUES ('first.last@example.com', 'x')");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (email, password_hash) VALUES ('First.Last@Example.com', 'y')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
