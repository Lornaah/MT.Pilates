package fr.mtpilates.team;

import java.util.List;
import java.util.UUID;

/** Ligne de l'écran Équipe. */
public record TeamMemberView(UUID id, String displayName, String email, String initial,
                             List<RoleBadge> roles, List<RoleOption> addableRoles) {

    public record RoleBadge(String code, String label, String variant, boolean removable) {
    }

    public record RoleOption(String code, String label) {
    }
}
