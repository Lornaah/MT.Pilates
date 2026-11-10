package fr.mtpilates.layout;

import fr.mtpilates.user.AppRole;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Libellé du rôle principal sous l'email dans le menu utilisateur (priorité de l'original). */
public final class PrimaryRoleLabel {

    private static final List<Map.Entry<AppRole, String>> PRIORITY = List.of(
            Map.entry(AppRole.OWNER, "Owner"),
            Map.entry(AppRole.MANAGER_COACH, "Manager coachs"),
            Map.entry(AppRole.MANAGER_SETTER, "Manager setters"),
            Map.entry(AppRole.MANAGER_CLOSER, "Manager closers"),
            Map.entry(AppRole.COACH, "Coach"),
            Map.entry(AppRole.SETTER, "Setter"),
            Map.entry(AppRole.CLOSER, "Closer"),
            Map.entry(AppRole.EXTERNAL_MEMBER, "Membre externe"));

    private PrimaryRoleLabel() {
    }

    public static String of(final Set<AppRole> roles) {
        return PRIORITY.stream()
                .filter(entry -> roles.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Membre");
    }
}
