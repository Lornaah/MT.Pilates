package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur connecté pour la requête en cours.
 *
 * @param realRoles      rôles attribués en base : servent aux règles d'accès aux données
 * @param effectiveRoles rôles vus par l'interface (remplacés par « Voir comme » chez un owner)
 */
public record CurrentUser(
        UUID id,
        String email,
        Optional<String> avatarUrl,
        Set<AppRole> realRoles,
        Set<AppRole> effectiveRoles,
        Optional<AppRole> viewAs,
        Capacities capacities) {

    public boolean isRealOwner() {
        return realRoles.contains(AppRole.OWNER);
    }

    public boolean hasRole(final AppRole role) {
        return effectiveRoles.contains(role);
    }

    public String initial() {
        return email.isEmpty() ? "?" : email.substring(0, 1);
    }
}
