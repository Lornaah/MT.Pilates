package fr.mtpilates.layout;

import static fr.mtpilates.layout.NavItem.exact;
import static fr.mtpilates.layout.NavItem.prefix;

import fr.mtpilates.auth.Capacities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Zones de la barre latérale déduites des capacités (port de navigation.ts) :
 * pratique (membres), activité (staff, ordonnée comme l'entonnoir commercial), administration (owner).
 */
public final class NavigationBuilder {

    private NavigationBuilder() {
    }

    public static List<NavZone> build(final Capacities caps) {
        return Stream.of(practiceZone(caps), activityZone(caps), adminZone(caps))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<NavZone> practiceZone(final Capacities caps) {
        if (caps.isExternalMember()) {
            return Optional.of(new NavZone("Mon espace", List.of(NavGroup.untitled(List.of(
                    exact("/app", "layout-dashboard", "nav_external_dashboard", "Tableau de bord"),
                    prefix("/app/program-readonly", "circle-play", "nav_external_program", "Mon programme"))))));
        }
        if (caps.isMember()) {
            return Optional.of(new NavZone("Mon suivi", List.of(NavGroup.untitled(List.of(
                    exact("/app", "layout-dashboard", "nav_member_program", "Mon programme"),
                    prefix("/app/videos", "circle-play", "nav_member_videos", "Mes séances"),
                    prefix("/app/tracking", "activity", "nav_member_tracking", "Mon suivi"),
                    prefix("/app/nutrition", "apple", "nav_member_nutrition", "Nutrition"),
                    prefix("/app/announcements", "megaphone", "nav_member_announcements", "Annonces"))))));
        }
        return Optional.empty();
    }

    private static Optional<NavZone> activityZone(final Capacities caps) {
        if (!caps.isStaff()) {
            return Optional.empty();
        }
        final List<NavGroup> groups = new ArrayList<>();
        groups.add(NavGroup.untitled(List.of(
                exact("/app", "layout-dashboard", "nav_dashboard", "Tableau de bord"),
                prefix("/app/calendar", "calendar", "nav_calendar", "Mon Calendrier"),
                prefix("/app/coach/announcements", "megaphone", "nav_announcements", "Annonces"))));
        if (caps.isCoach() || caps.canManageCoaches()) {
            groups.add(NavGroup.titled("Coaching", List.of(
                    prefix("/app/coach/members", "users", "nav_members", "Mes coachés"),
                    prefix("/app/coach/training", "dumbbell", "nav_training", "Programme"),
                    prefix("/app/coach/nutrition", "apple", "nav_nutrition", "Nutrition"),
                    prefix("/app/coach/habits", "sparkles", "nav_habits", "Habitudes"))));
            groups.add(NavGroup.titled("Bibliothèque", List.of(
                    prefix("/app/coach/videos", "video", "nav_videos", "Vidéos"),
                    prefix("/app/coach/documents", "file-text", "nav_documents", "Documents"))));
        }
        if (caps.isSetter() || caps.isCloser() || caps.canManageSetters() || caps.canManageClosers()) {
            groups.add(NavGroup.titled("Ventes", salesItems(caps)));
        }
        return Optional.of(new NavZone("Activité", groups));
    }

    private static List<NavItem> salesItems(final Capacities caps) {
        final List<NavItem> items = new ArrayList<>(List.of(
                prefix("/app/prospects", "trending-up", "nav_prospects", "Leads"),
                prefix("/app/calls", "phone-call", "nav_calls", "Closing")));
        if (caps.isSetter() && !caps.isOwner()) {
            items.add(prefix("/app/my-booking-link", "link", "nav_my_link", "Mon lien"));
        }
        return items;
    }

    private static Optional<NavZone> adminZone(final Capacities caps) {
        if (!caps.isOwner()) {
            return Optional.empty();
        }
        return Optional.of(new NavZone("Administration", List.of(NavGroup.untitled(List.of(
                prefix("/app/money", "banknote", "nav_money", "Finance"),
                prefix("/app/admin/team", "badge-check", "nav_admin_team", "Équipe"),
                prefix("/app/owner/settings", "settings", "nav_admin_settings", "Configuration"))))));
    }
}
