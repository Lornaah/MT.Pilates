package fr.mtpilates.config;

import java.util.List;

/**
 * Sections de l'écran Configuration (settings-config.ts de l'original). La section
 * « Titres de pages » n'est pas reprise : ses clés n'étaient lues nulle part (décision Q10).
 */
public final class SettingsCatalog {

    public record Key(String key, String label) {
    }

    public record Group(String subtitle, List<Key> keys) {
    }

    public record Section(String id, String icon, String title, String description, List<Group> groups) {
    }

    public static final List<Section> SECTIONS = List.of(
            new Section("nav", "navigation", "Navigation", "Libellés de la barre latérale", List.of(
                    new Group("Membres", List.of(
                            new Key("nav_member_program", "Mon programme"), new Key("nav_member_videos", "Mes séances"),
                            new Key("nav_member_tracking", "Mon suivi"), new Key("nav_member_nutrition", "Nutrition"),
                            new Key("nav_member_announcements", "Annonces"), new Key("nav_member_profile", "Ma fiche"))),
                    new Group("Activité — Coaching", List.of(
                            new Key("nav_dashboard", "Tableau de bord"), new Key("nav_calendar", "Calendrier"),
                            new Key("nav_members", "Coachés"), new Key("nav_training", "Programme"),
                            new Key("nav_nutrition", "Nutrition"), new Key("nav_habits", "Habitudes"),
                            new Key("nav_videos", "Vidéos"), new Key("nav_announcements", "Annonces"),
                            new Key("nav_documents", "Documents"))),
                    new Group("Activité — Ventes", List.of(
                            new Key("nav_prospects", "Prospects"), new Key("nav_calls", "Appels"),
                            new Key("nav_my_link", "Mon lien"), new Key("nav_money", "Money"))),
                    new Group("Administration", List.of(
                            new Key("nav_admin_team", "Équipe"), new Key("nav_admin_settings", "Configuration"))))),
            new Section("roles", "users", "Rôles & statuts", "Noms affichés pour les rôles et statuts", List.of(
                    new Group("Rôles", List.of(
                            new Key("role_label_owner", "Owner"), new Key("role_label_coach", "Coach"),
                            new Key("role_label_setter", "Setter"), new Key("role_label_closer", "Closer"),
                            new Key("role_label_manager_coach", "Manager coachs"),
                            new Key("role_label_manager_setter", "Manager setters"),
                            new Key("role_label_manager_closer", "Manager closers"),
                            new Key("role_label_member", "Membre"),
                            new Key("role_label_external_member", "Membre externe"))),
                    new Group("Statuts membres", List.of(
                            new Key("status_label_active", "Actif"), new Key("status_label_paused", "En pause"),
                            new Key("status_label_archived", "Archivé"), new Key("status_label_finished", "Terminé"))))),
            new Section("prospects", "megaphone", "Prospects", "Pipeline commercial et sources d'acquisition", List.of(
                    new Group("Colonnes Kanban", List.of(
                            new Key("prospect_col_new", "Colonne Nouveaux"), new Key("prospect_col_to_call", "Colonne À rappeler"),
                            new Key("prospect_col_trial", "Colonne En essai"), new Key("prospect_col_converted", "Colonne Convertis"))),
                    new Group("Statuts", List.of(
                            new Key("prospect_status_new", "Nouveau"), new Key("prospect_status_to_call", "À rappeler"),
                            new Key("prospect_status_trial", "En essai"), new Key("prospect_status_converted", "Converti"),
                            new Key("prospect_status_lost", "Perdu"))),
                    new Group("Sources", List.of(
                            new Key("source_label_instagram", "Instagram"), new Key("source_label_tiktok", "TikTok"),
                            new Key("source_label_website", "Site web"), new Key("source_label_word_of_mouth", "Bouche-à-oreille"),
                            new Key("source_label_referral", "Recommandation"), new Key("source_label_other", "Autre"))))),
            new Section("videos", "video", "Vidéos", "Niveaux de difficulté", List.of(
                    new Group("Niveaux", List.of(
                            new Key("video_level_beginner", "Débutant"), new Key("video_level_intermediate", "Intermédiaire"),
                            new Key("video_level_advanced", "Avancé"))))),
            new Section("events", "calendar", "Événements", "Types de séances du calendrier", List.of(
                    new Group("Types", List.of(
                            new Key("event_type_live_class", "Cours en direct"), new Key("event_type_studio_class", "Cours studio"),
                            new Key("event_type_one_on_one", "Séance individuelle"), new Key("event_type_call", "Appel"),
                            new Key("event_type_admin_task", "Tâche admin"))))),
            new Section("onboarding", "circle-play", "Onboarding", "Parcours de première connexion membre", List.of(
                    new Group("Vidéo d'introduction", List.of(
                            new Key("onboarding_video_url", "URL vidéo (YouTube ou Vimeo)"))))));

    private SettingsCatalog() {
    }

    public static List<String> allKeys() {
        return SECTIONS.stream()
                .flatMap(section -> section.groups().stream())
                .flatMap(group -> group.keys().stream())
                .map(Key::key)
                .toList();
    }
}
