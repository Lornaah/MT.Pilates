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
    private final SidebarFactory sidebars;

    public AppLayoutAdvice(final CurrentUserService currentUsers, final ConfigService config,
                           final SidebarFactory sidebars) {
        this.currentUsers = currentUsers;
        this.config = config;
        this.sidebars = sidebars;
    }

    /** Libellés configurables (statuts, rôles, sources…), utilisés par les fragments communs. */
    @ModelAttribute("labels")
    public Labels labels() {
        return config.labels();
    }

    @ModelAttribute("currentUser")
    public Optional<CurrentUser> currentUser() {
        return currentUsers.current();
    }

    @ModelAttribute("sidebar")
    public Optional<SidebarView> sidebar(final HttpServletRequest request) {
        return currentUsers.current().map(user -> sidebars.build(user, config.labels(), request.getRequestURI()));
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
}
