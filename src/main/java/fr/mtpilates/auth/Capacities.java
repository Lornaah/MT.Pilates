package fr.mtpilates.auth;

import static fr.mtpilates.user.AppRole.CLOSER;
import static fr.mtpilates.user.AppRole.COACH;
import static fr.mtpilates.user.AppRole.EXTERNAL_MEMBER;
import static fr.mtpilates.user.AppRole.MANAGER_CLOSER;
import static fr.mtpilates.user.AppRole.MANAGER_COACH;
import static fr.mtpilates.user.AppRole.MANAGER_SETTER;
import static fr.mtpilates.user.AppRole.MEMBER;
import static fr.mtpilates.user.AppRole.OWNER;
import static fr.mtpilates.user.AppRole.SETTER;

import fr.mtpilates.user.AppRole;
import java.util.Set;

/**
 * Capacités d'interface, port exact de {@code deriveCapacities} (useAuth.tsx) :
 * elles décident des menus et des écrans accessibles. Owner est un super-rôle.
 * Attention : l'accès aux données suit d'autres règles (voir {@link DataAccessRoles}).
 */
public record Capacities(
        boolean isMember,
        boolean isExternalMember,
        boolean isCoach,
        boolean isSetter,
        boolean isCloser,
        boolean isManager,
        boolean isOwner,
        boolean isManagerCoach,
        boolean isManagerSetter,
        boolean isManagerCloser,
        boolean isStaff,
        boolean isOperational,
        boolean canManageCoaches,
        boolean canManageSetters,
        boolean canManageClosers) {

    public static Capacities of(final Set<AppRole> roles) {
        final boolean owner = roles.contains(OWNER);
        final boolean coach = roles.contains(COACH) || owner;
        final boolean setter = roles.contains(SETTER) || owner;
        final boolean closer = roles.contains(CLOSER) || owner;
        final boolean managerCoach = roles.contains(MANAGER_COACH) || owner;
        final boolean managerSetter = roles.contains(MANAGER_SETTER) || owner;
        final boolean managerCloser = roles.contains(MANAGER_CLOSER) || owner;
        final boolean manager = managerCoach || managerSetter || managerCloser;
        final boolean externalMember = roles.contains(EXTERNAL_MEMBER);
        final boolean staff = coach || setter || closer || manager;
        // Un utilisateur sans aucun rôle (nouveau compte historique) est traité comme membre.
        final boolean member = roles.contains(MEMBER) || (!staff && !externalMember);
        return new Capacities(member, externalMember, coach, setter, closer, manager, owner,
                managerCoach, managerSetter, managerCloser, staff, coach || setter || closer,
                managerCoach, managerSetter, managerCloser);
    }
}
