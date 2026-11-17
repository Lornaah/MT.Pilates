package fr.mtpilates.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instantané de la configuration, avec les libellés par défaut de l'original
 * (useAppConfig.label et useLabels). Une valeur vide renvoie au défaut : c'est ainsi
 * que « Réinitialiser » restaure les libellés.
 */
public record Labels(Map<String, String> values) {

    public static final String DEFAULT_OBJECTIVES =
            "Endométriose,Règles douloureuses,Handicap,Perte de poids,Prise de masse,Renforcement,Mobilité,Pré/post-natal,Bien-être";

    private static final Map<String, String> STATUS = Map.of(
            "active", "Actif", "paused", "En pause", "archived", "Archivé", "finished", "Terminé");
    private static final Map<String, String> EVENT_TYPE = Map.of(
            "live_class", "Cours en ligne", "studio_class", "Cours studio", "one_on_one", "Suivi individuel",
            "call", "Appel", "admin_task", "Premier RDV");
    private static final Map<String, String> VIDEO_LEVEL = Map.of(
            "beginner", "Débutante", "intermediate", "Intermédiaire", "advanced", "Avancée");
    private static final Map<String, String> ROLE = Map.ofEntries(
            Map.entry("owner", "Owner"), Map.entry("setter", "Setter"), Map.entry("coach", "Coach"),
            Map.entry("member", "Membre"), Map.entry("admin", "Admin"), Map.entry("user", "Utilisateur"),
            Map.entry("external_member", "Membre externe"), Map.entry("manager_coach", "Manager coach"),
            Map.entry("manager_setter", "Manager setter"), Map.entry("manager_closer", "Manager closer"),
            Map.entry("closer", "Closer"));
    private static final Map<String, String> PROSPECT_SOURCE = Map.of(
            "instagram", "Instagram", "tiktok", "TikTok", "website", "Site web",
            "word_of_mouth", "Bouche-à-oreille", "referral", "Recommandation", "other", "Autre");
    private static final Map<String, String> PROSPECT_STATUS = Map.of(
            "new", "Nouveau", "to_call", "À rappeler", "trial", "En essai", "converted", "Converti", "lost", "Perdu");
    private static final Map<String, String> PROSPECT_COLUMN = Map.of(
            "new", "Nouveaux", "to_call", "À rappeler", "trial", "En essai", "converted", "Convertis", "lost", "Perdus");

    public Labels {
        values = Map.copyOf(values);
    }

    public String label(final String key, final String fallback) {
        final String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public String status(final String status) {
        return byPrefix("status_label_", STATUS, status);
    }

    public String eventType(final String type) {
        return byPrefix("event_type_", EVENT_TYPE, type);
    }

    public String videoLevel(final String level) {
        return byPrefix("video_level_", VIDEO_LEVEL, level);
    }

    public String role(final String role) {
        return byPrefix("role_label_", ROLE, role);
    }

    public String prospectSource(final String source) {
        return byPrefix("source_label_", PROSPECT_SOURCE, source);
    }

    public String prospectStatus(final String status) {
        return byPrefix("prospect_status_", PROSPECT_STATUS, status);
    }

    public String prospectColumn(final String status) {
        return byPrefix("prospect_col_", PROSPECT_COLUMN, status);
    }

    /** Objectifs proposés aux membres et programmes (liste CSV de la clé program_objectives). */
    public List<String> objectives() {
        return Arrays.stream(label("program_objectives", DEFAULT_OBJECTIVES).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Valeur brute éditée dans l'écran Configuration ("" si absente). */
    public String raw(final String key) {
        return values.getOrDefault(key, "");
    }

    private String byPrefix(final String prefix, final Map<String, String> fallbacks, final String value) {
        return label(prefix + value, fallbacks.getOrDefault(value, value));
    }
}
