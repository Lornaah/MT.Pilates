package fr.mtpilates.team;

import fr.mtpilates.user.AppRole;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Présentation des rôles sur l'écran Équipe (ASSIGNABLE_ROLES et ROLE_PRESENTATION de l'original). */
final class TeamRoles {

    /** Rôles qui font apparaître quelqu'un dans l'équipe. */
    static final Set<AppRole> STAFF = EnumSet.of(AppRole.OWNER, AppRole.COACH, AppRole.SETTER, AppRole.CLOSER,
            AppRole.MANAGER_COACH, AppRole.MANAGER_SETTER, AppRole.MANAGER_CLOSER);

    /** Rôles attribuables depuis l'écran, dans l'ordre du menu. Owner n'en fait pas partie. */
    static final Map<AppRole, String> ASSIGNABLE = labels(
            AppRole.COACH, "Coach", AppRole.SETTER, "Setter", AppRole.CLOSER, "Closer",
            AppRole.MANAGER_COACH, "Manager coachs", AppRole.MANAGER_SETTER, "Manager setters",
            AppRole.MANAGER_CLOSER, "Manager closers", AppRole.EXTERNAL_MEMBER, "Membre externe",
            AppRole.MEMBER, "Membre");

    private TeamRoles() {
    }

    static String label(final AppRole role) {
        return role == AppRole.OWNER ? "Owner" : ASSIGNABLE.getOrDefault(role, role.code());
    }

    static String variant(final AppRole role) {
        return switch (role) {
            case OWNER -> "default";
            case COACH, SETTER, CLOSER -> "secondary";
            default -> "outline";
        };
    }

    private static Map<AppRole, String> labels(final Object... pairs) {
        final Map<AppRole, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((AppRole) pairs[i], (String) pairs[i + 1]);
        }
        return map;
    }
}
