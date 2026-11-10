package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import jakarta.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Résout l'utilisateur connecté, ses rôles réels et ses rôles effectifs (« Voir comme »). */
@Service
public class CurrentUserService {

    static final String VIEW_AS_SESSION_KEY = "mtpilates.viewAs";
    private static final String REQUEST_CACHE_KEY = CurrentUserService.class.getName();

    private final UserRoleRepository roles;
    private final ProfileRepository profiles;

    public CurrentUserService(final UserRoleRepository roles, final ProfileRepository profiles) {
        this.roles = roles;
        this.profiles = profiles;
    }

    /** Utilisateur connecté, calculé une seule fois par requête. */
    @Transactional(readOnly = true)
    public Optional<CurrentUser> current() {
        final RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null && request.getAttribute(REQUEST_CACHE_KEY, RequestAttributes.SCOPE_REQUEST) instanceof CurrentUser cached) {
            return Optional.of(cached);
        }
        final Optional<CurrentUser> resolved = principal().map(p -> resolve(p, viewAs()));
        resolved.ifPresent(user -> {
            if (request != null) {
                request.setAttribute(REQUEST_CACHE_KEY, user, RequestAttributes.SCOPE_REQUEST);
            }
        });
        return resolved;
    }

    /** Variante pour les points d'entrée réservés aux utilisateurs connectés. */
    public CurrentUser require() {
        return current().orElseThrow(() -> new IllegalStateException("Aucun utilisateur connecté"));
    }

    public Set<AppRole> rolesOf(final UUID userId) {
        return roles.findByUserId(userId).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AppRole.class)));
    }

    public void setViewAs(final HttpSession session, final Optional<ViewAsRole> viewAs) {
        viewAs.ifPresentOrElse(
                role -> session.setAttribute(VIEW_AS_SESSION_KEY, role.role()),
                () -> session.removeAttribute(VIEW_AS_SESSION_KEY));
    }

    private CurrentUser resolve(final AuthenticatedUser principal, final Optional<AppRole> requestedViewAs) {
        final Set<AppRole> real = rolesOf(principal.getId());
        // Seul un owner (réel) peut endosser un autre rôle ; sinon la valeur de session est ignorée.
        final Optional<AppRole> viewAs = real.contains(AppRole.OWNER) ? requestedViewAs : Optional.empty();
        final Set<AppRole> effective = viewAs.map(role -> (Set<AppRole>) EnumSet.of(role)).orElse(real);
        final Optional<String> avatar = profiles.findById(principal.getId()).flatMap(Profile::getAvatarUrl);
        return new CurrentUser(principal.getId(), principal.getUsername(), avatar, real, effective, viewAs,
                Capacities.of(effective));
    }

    private static Optional<AuthenticatedUser> principal() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? Optional.of(user)
                : Optional.empty();
    }

    private static Optional<AppRole> viewAs() {
        final RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request == null) {
            return Optional.empty();
        }
        return request.getAttribute(VIEW_AS_SESSION_KEY, RequestAttributes.SCOPE_SESSION) instanceof AppRole role
                ? Optional.of(role)
                : Optional.empty();
    }
}
