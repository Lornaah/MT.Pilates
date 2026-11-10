package fr.mtpilates.layout;

/** Entrée de menu : `exact` = l'entrée n'est active que sur ce chemin exact (sinon : préfixe). */
public record NavItem(String path, String icon, String labelKey, String labelFallback, boolean exact) {

    static NavItem prefix(final String path, final String icon, final String labelKey, final String fallback) {
        return new NavItem(path, icon, labelKey, fallback, false);
    }

    static NavItem exact(final String path, final String icon, final String labelKey, final String fallback) {
        return new NavItem(path, icon, labelKey, fallback, true);
    }

    boolean isActive(final String requestPath) {
        return exact ? requestPath.equals(path) : requestPath.startsWith(path);
    }
}
