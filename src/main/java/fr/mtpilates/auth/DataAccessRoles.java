package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import java.util.Set;

/**
 * Rôles d'accès aux données, port des fonctions SQL de l'original (décision Q1 :
 * la base fait foi). Toujours évaluées sur les rôles réels, jamais sur « Voir comme ».
 */
public final class DataAccessRoles {

    private DataAccessRoles() {
    }

    /** {@code is_staff()} : owner, setter ou coach (closers et managers n'en font pas partie). */
    public static boolean isStaff(final Set<AppRole> roles) {
        return roles.contains(AppRole.OWNER) || roles.contains(AppRole.SETTER) || roles.contains(AppRole.COACH);
    }

    /** {@code is_management()} : owner ou setter. */
    public static boolean isManagement(final Set<AppRole> roles) {
        return roles.contains(AppRole.OWNER) || roles.contains(AppRole.SETTER);
    }
}
