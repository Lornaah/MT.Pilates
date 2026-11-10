package fr.mtpilates.layout;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.auth.ViewAsRole;
import fr.mtpilates.config.Labels;
import fr.mtpilates.config.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Données du layout applicatif (barre latérale, menu utilisateur, bandeau « Voir comme »). */
@ControllerAdvice
public class AppLayoutAdvice {

    private final CurrentUserService currentUsers;
    private final ConfigService config;

    public AppLayoutAdvice(final CurrentUserService currentUsers, final ConfigService config) {
        this.currentUsers = currentUsers;
        this.config = config;
    }

    @ModelAttribute("currentUser")
    public Optional<CurrentUser> currentUser() {
        return currentUsers.current();
    }

    @ModelAttribute("sidebar")
    public Optional<SidebarView> sidebar(final HttpServletRequest request) {
        return currentUsers.current().map(user -> toView(user, config.labels(), request.getRequestURI()));
    }

    @ModelAttribute("viewAsOptions")
    public List<ViewAsRole> viewAsOptions() {
        return ViewAsRole.options();
    }

    /** Libellé du bandeau « Voir comme », présent seulement quand un owner endosse un autre rôle. */
    @ModelAttribute("viewAsBannerLabel")
    public Optional<String> viewAsBannerLabel() {
        return currentUsers.current()
                .filter(CurrentUser::isRealOwner)
                .flatMap(CurrentUser::viewAs)
                .flatMap(ViewAsRole::of)
                .map(ViewAsRole::bannerLabel);
    }

    @ModelAttribute("primaryRoleLabel")
    public Optional<String> primaryRoleLabel() {
        return currentUsers.current().map(user -> PrimaryRoleLabel.of(user.effectiveRoles()));
    }

    private static SidebarView toView(final CurrentUser user, final Labels labels, final String path) {
        return new SidebarView(NavigationBuilder.build(user.capacities()).stream()
                .map(zone -> new SidebarView.Zone(zone.title(), zone.groups().stream()
                        .map(group -> new SidebarView.Group(group.title(), group.items().stream()
                                .map(item -> new SidebarView.Link(item.path(), item.icon(),
                                        labels.label(item.labelKey(), item.labelFallback()), item.isActive(path)))
                                .toList()))
                        .toList()))
                .toList());
    }
}
