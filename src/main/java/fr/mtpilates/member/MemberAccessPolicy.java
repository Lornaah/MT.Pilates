package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Règles d'accès aux fiches (policies RLS de `profiles`, décision Q1) : évaluées sur les rôles réels.
 * Lecture et modification : soi-même, le management (owner, setter) ou le coach assigné. Création et
 * suppression : management.
 */
@Component
public class MemberAccessPolicy {

    public boolean canView(final CurrentUser user, final UUID memberId, final Optional<UUID> memberCoachId) {
        return user.id().equals(memberId) || isManagement(user) || memberCoachId.filter(user.id()::equals).isPresent();
    }

    public boolean canUpdate(final CurrentUser user, final UUID memberId, final Optional<UUID> memberCoachId) {
        return canView(user, memberId, memberCoachId);
    }

    public boolean canCreate(final CurrentUser user) {
        return isManagement(user);
    }

    /**
     * Suppression par le management. On refuse en plus de supprimer son propre compte ou celui d'un owner :
     * l'original le permettait et laissait l'application sans session valide ou sans propriétaire (Q2).
     */
    public boolean canDelete(final CurrentUser user, final UUID memberId, final boolean memberIsOwner) {
        return isManagement(user) && !user.id().equals(memberId) && !memberIsOwner;
    }

    private static boolean isManagement(final CurrentUser user) {
        return DataAccessRoles.isManagement(user.realRoles());
    }
}
