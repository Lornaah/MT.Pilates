package fr.mtpilates.shared.security;

import fr.mtpilates.shared.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

/** Accès refusé à une page faute de rôle : retour à /app, comme ProtectedRoute dans l'original. */
public class RedirectToAppHomeHandler implements AccessDeniedHandler {

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final AccessDeniedException exception) throws IOException {
        if (exception instanceof CsrfException) {
            // Jeton CSRF absent ou périmé : refus net, ce n'est pas une question de rôle.
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        final String target = request.getContextPath() + "/app";
        if (HtmxRequest.isHtmx(request)) {
            response.setHeader("HX-Redirect", target);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.sendRedirect(target);
    }
}
