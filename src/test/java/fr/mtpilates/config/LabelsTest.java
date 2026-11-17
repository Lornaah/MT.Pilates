package fr.mtpilates.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LabelsTest {

    @Test
    void usesTheConfiguredValueAndFallsBackWhenAbsentOrEmpty() {
        final Labels labels = new Labels(Map.of("status_label_active", "En cours", "status_label_paused", ""));

        assertThat(labels.status("active")).isEqualTo("En cours");
        assertThat(labels.status("paused")).isEqualTo("En pause");
        assertThat(labels.status("finished")).isEqualTo("Terminé");
        assertThat(labels.status("inconnu")).isEqualTo("inconnu");
    }

    @Test
    void exposesTheOriginalFallbacksForEveryFamily() {
        final Labels labels = new Labels(Map.of());

        assertThat(labels.eventType("admin_task")).isEqualTo("Premier RDV");
        assertThat(labels.videoLevel("beginner")).isEqualTo("Débutante");
        assertThat(labels.role("manager_coach")).isEqualTo("Manager coach");
        assertThat(labels.prospectSource("word_of_mouth")).isEqualTo("Bouche-à-oreille");
        assertThat(labels.prospectStatus("to_call")).isEqualTo("À rappeler");
        assertThat(labels.prospectColumn("lost")).isEqualTo("Perdus");
    }

    @Test
    void splitsTheObjectivesListAndIgnoresBlanks() {
        assertThat(new Labels(Map.of("program_objectives", " Mobilité , ,Bien-être")).objectives())
                .containsExactly("Mobilité", "Bien-être");
        assertThat(new Labels(Map.of("program_objectives", "")).objectives()).hasSize(9).startsWith("Endométriose");
    }

    @Test
    void settingsCatalogHasNoPageTitleKeys() {
        assertThat(SettingsCatalog.allKeys()).contains("nav_calendar", "onboarding_video_url")
                .noneMatch(key -> key.startsWith("page_title_"));
    }
}
