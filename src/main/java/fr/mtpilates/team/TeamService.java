package fr.mtpilates.team;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestion des rôles de l'équipe. Règle de la base d'origine : seul un owner (réel) modifie user_roles. */
@Service
public class TeamService {

    static final String OWNER_NOT_REMOVABLE = "Le rôle owner ne peut pas être retiré.";

    private final ProfileRepository profiles;
    private final UserRoleRepository roles;

    public TeamService(final ProfileRepository profiles, final UserRoleRepository roles) {
        this.profiles = profiles;
        this.roles = roles;
    }

    /** Membres ayant au moins un rôle staff, filtrés comme dans l'original (texte + rôle). */
    @Transactional(readOnly = true)
    public List<TeamMemberView> list(final String search, final Optional<AppRole> roleFilter) {
        final Map<UUID, List<AppRole>> rolesByUser = roles.findAll().stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(UserRole::getRole, Collectors.toList())));
        final String needle = search == null ? "" : search.toLowerCase(Locale.ROOT);
        return profiles.findAllByOrderByCreatedAtDesc().stream()
                .map(profile -> Map.entry(profile, rolesByUser.getOrDefault(profile.getId(), List.of())))
                .filter(entry -> entry.getValue().stream().anyMatch(TeamRoles.STAFF::contains))
                .filter(entry -> needle.isEmpty() || searchable(entry.getKey()).contains(needle))
                .filter(entry -> roleFilter.map(entry.getValue()::contains).orElse(true))
                .map(entry -> toView(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public void addRole(final CurrentUser actor, final UUID userId, final AppRole role) {
        requireOwner(actor);
        if (!TeamRoles.ASSIGNABLE.containsKey(role)) {
            throw new IllegalArgumentException("Rôle non attribuable : " + role.code());
        }
        if (!roles.existsByUserIdAndRole(userId, role)) {
            roles.save(new UserRole(userId, role));
        }
    }

    @Transactional
    public void removeRole(final CurrentUser actor, final UUID userId, final AppRole role) {
        requireOwner(actor);
        if (role == AppRole.OWNER) {
            throw new IllegalArgumentException(OWNER_NOT_REMOVABLE);
        }
        roles.deleteByUserIdAndRole(userId, role);
    }

    private static void requireOwner(final CurrentUser actor) {
        if (!actor.isRealOwner()) {
            throw new AccessDeniedException("Réservé à l'owner");
        }
    }

    private static String searchable(final Profile profile) {
        return (profile.getFirstName().orElse("") + " " + profile.getLastName().orElse("") + " " + profile.getEmail())
                .toLowerCase(Locale.ROOT);
    }

    private static TeamMemberView toView(final Profile profile, final List<AppRole> userRoles) {
        final String name = (profile.getFirstName().orElse("") + " " + profile.getLastName().orElse("")).trim();
        final String initial = profile.getFirstName().filter(s -> !s.isEmpty()).map(s -> s.substring(0, 1))
                .orElseGet(() -> profile.getEmail().isEmpty() ? "?" : profile.getEmail().substring(0, 1))
                .toUpperCase(Locale.ROOT);
        return new TeamMemberView(profile.getId(), name.isEmpty() ? profile.getEmail() : name, profile.getEmail(), initial,
                userRoles.stream()
                        .map(role -> new TeamMemberView.RoleBadge(role.code(), TeamRoles.label(role), TeamRoles.variant(role),
                                role != AppRole.OWNER))
                        .toList(),
                TeamRoles.ASSIGNABLE.entrySet().stream()
                        .filter(option -> !userRoles.contains(option.getKey()))
                        .map(option -> new TeamMemberView.RoleOption(option.getKey().code(), option.getValue()))
                        .toList());
    }
}
