package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import java.util.List;
import java.util.Optional;

/** Rôles proposés dans le sélecteur « Voir comme… » de l'owner (liste de l'original). */
public enum ViewAsRole {
    COACH(AppRole.COACH, "Coach", "coach"),
    SETTER(AppRole.SETTER, "Setter", "setter"),
    CLOSER(AppRole.CLOSER, "Closer", "closer"),
    MANAGER_COACH(AppRole.MANAGER_COACH, "Manager coachs", "manager coachs"),
    MEMBER(AppRole.MEMBER, "Membre", "membre"),
    EXTERNAL_MEMBER(AppRole.EXTERNAL_MEMBER, "Membre externe", "membre externe");

    private final AppRole role;
    private final String optionLabel;
    private final String bannerLabel;

    ViewAsRole(final AppRole role, final String optionLabel, final String bannerLabel) {
        this.role = role;
        this.optionLabel = optionLabel;
        this.bannerLabel = bannerLabel;
    }

    public static List<ViewAsRole> options() {
        return List.of(values());
    }

    public static Optional<ViewAsRole> of(final AppRole role) {
        return options().stream().filter(v -> v.role == role).findFirst();
    }

    public AppRole role() {
        return role;
    }

    public String code() {
        return role.code();
    }

    public String optionLabel() {
        return optionLabel;
    }

    public String bannerLabel() {
        return bannerLabel;
    }
}
