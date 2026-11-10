package fr.mtpilates.shared.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lecture des en-têtes HTMX. Les controllers s'en servent pour choisir entre
 * une page complète et un fragment.
 */
public final class HtmxRequest {

    public static final String HX_REQUEST = "HX-Request";
    public static final String HX_BOOSTED = "HX-Boosted";

    private HtmxRequest() {
    }

    public static boolean isHtmx(final HttpServletRequest request) {
        return "true".equals(request.getHeader(HX_REQUEST));
    }

    /** Requête HTMX qui attend un fragment (une navigation « boostée » attend une page complète). */
    public static boolean wantsFragment(final HttpServletRequest request) {
        return "true".equals(request.getHeader(HX_REQUEST))
                && !"true".equals(request.getHeader(HX_BOOSTED));
    }

    /** Renvoie la vue fragment si la requête vient d'HTMX, sinon la page complète. */
    public static String view(final HttpServletRequest request, final String page, final String fragment) {
        return wantsFragment(request) ? fragment : page;
    }
}
