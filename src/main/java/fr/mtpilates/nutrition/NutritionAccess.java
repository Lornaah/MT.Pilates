package fr.mtpilates.nutrition;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import org.springframework.security.access.AccessDeniedException;

/** Règle commune des bibliothèques nutrition (policies « staff » sur foods, recipes et meal_plan_templates). */
final class NutritionAccess {

    private NutritionAccess() {
    }

    static void requireStaff(final CurrentUser user) {
        if (!DataAccessRoles.isStaff(user.realRoles())) {
            throw new AccessDeniedException("Réservé au staff");
        }
    }
}
