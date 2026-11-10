package fr.mtpilates.layout;

import static org.assertj.core.api.Assertions.assertThat;

import fr.mtpilates.auth.Capacities;
import fr.mtpilates.user.AppRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NavigationBuilderTest {

    @Test
    void memberSeesOnlyTheirFollowUpZone() {
        final List<NavZone> zones = NavigationBuilder.build(Capacities.of(EnumSet.of(AppRole.MEMBER)));

        assertThat(zones).extracting(NavZone::title).containsExactly("Mon suivi");
        assertThat(paths(zones)).containsExactly("/app", "/app/videos", "/app/tracking", "/app/nutrition", "/app/announcements");
    }

    @Test
    void ownerSeesActivityAndAdministrationWithoutTheSetterLink() {
        final List<NavZone> zones = NavigationBuilder.build(Capacities.of(EnumSet.of(AppRole.OWNER)));

        assertThat(zones).extracting(NavZone::title).containsExactly("Activité", "Administration");
        assertThat(paths(zones)).contains("/app/coach/members", "/app/prospects", "/app/money", "/app/admin/team")
                .doesNotContain("/app/my-booking-link");
    }

    @Test
    void setterGetsTheSalesGroupWithTheirBookingLinkButNoCoaching() {
        final List<NavZone> zones = NavigationBuilder.build(Capacities.of(EnumSet.of(AppRole.SETTER)));

        assertThat(paths(zones)).contains("/app/prospects", "/app/calls", "/app/my-booking-link")
                .doesNotContain("/app/coach/members");
    }

    @Test
    void externalMemberSeesTheirRestrictedSpace() {
        final List<NavZone> zones = NavigationBuilder.build(Capacities.of(Set.of(AppRole.EXTERNAL_MEMBER)));

        assertThat(zones).extracting(NavZone::title).containsExactly("Mon espace");
    }

    @Test
    void dashboardLinkIsActiveOnlyOnItsExactPath() {
        final NavItem dashboard = NavItem.exact("/app", "layout-dashboard", "nav_dashboard", "Tableau de bord");
        final NavItem videos = NavItem.prefix("/app/coach/videos", "video", "nav_videos", "Vidéos");

        assertThat(dashboard.isActive("/app")).isTrue();
        assertThat(dashboard.isActive("/app/coach/videos")).isFalse();
        assertThat(videos.isActive("/app/coach/videos/123")).isTrue();
    }

    @Test
    void primaryRoleLabelFollowsTheOriginalPriority() {
        assertThat(PrimaryRoleLabel.of(EnumSet.of(AppRole.COACH, AppRole.MANAGER_SETTER))).isEqualTo("Manager setters");
        assertThat(PrimaryRoleLabel.of(EnumSet.of(AppRole.MEMBER))).isEqualTo("Membre");
    }

    private static List<String> paths(final List<NavZone> zones) {
        return zones.stream().flatMap(z -> z.groups().stream()).flatMap(g -> g.items().stream()).map(NavItem::path).toList();
    }
}
