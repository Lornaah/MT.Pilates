package fr.mtpilates.auth;

import fr.mtpilates.shared.web.Flash;
import fr.mtpilates.shared.web.Toast;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Après connexion : toast « Bienvenue ! » et arrivée sur /app, comme l'original (qui ignorait la page demandée). */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    static final String WELCOME = "Bienvenue !";

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws IOException {
        Flash.put(request, response, "/app", Map.of("toast", Toast.success(WELCOME)));
        response.sendRedirect(request.getContextPath() + "/app");
    }
}
