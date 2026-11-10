package fr.mtpilates.shared.security;

import fr.mtpilates.shared.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Renvoie vers la page de connexion. Pour une requête HTMX, une redirection HTTP serait
 * suivie en arrière-plan et la page de connexion injectée dans un fragment : on demande
 * plutôt à HTMX de naviguer.
 */
public class HtmxAwareEntryPoint implements AuthenticationEntryPoint {

    private final String loginPath;

    public HtmxAwareEntryPoint(final String loginPath) {
        this.loginPath = loginPath;
    }

    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response,
                         final AuthenticationException exception) throws IOException {
        final String target = request.getContextPath() + loginPath;
        if (HtmxRequest.isHtmx(request)) {
            response.setHeader("HX-Redirect", target);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        response.sendRedirect(target);
    }
}
