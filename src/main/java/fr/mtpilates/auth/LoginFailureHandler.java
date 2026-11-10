package fr.mtpilates.auth;

import fr.mtpilates.shared.web.Flash;
import fr.mtpilates.shared.web.Toast;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Traduit l'échec de connexion en message français, comme authService.translateAuthError.
 * Mauvais mot de passe et compte inconnu donnent le même message, pour ne pas révéler les comptes.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    static final String INVALID_CREDENTIALS = "Email ou mot de passe incorrect";
    static final String NOT_CONFIRMED = "Email pas encore confirmé. Vérifiez votre boîte de réception (et les spams).";
    static final String PASSWORD_REQUIRED = "Mot de passe requis";
    static final String PASSWORD_TOO_LONG = "Mot de passe trop long";

    private final RegistrationService registrations;

    public LoginFailureHandler(final RegistrationService registrations) {
        this.registrations = registrations;
    }

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
                                        final AuthenticationException exception) throws IOException {
        final String email = AccountUserDetailsService.normalize(request.getParameter("email"));
        final String password = Optional.ofNullable(request.getParameter("password")).orElse("");
        final Optional<String> formError = formError(email, password);
        final boolean unconfirmed = formError.isEmpty() && registrations.isAwaitingConfirmation(email, password);
        final String message = formError.orElse(unconfirmed ? NOT_CONFIRMED : INVALID_CREDENTIALS);

        final Map<String, Object> flash = new HashMap<>();
        flash.put("toast", Toast.error(message));
        flash.put("email", email);
        if (unconfirmed) {
            flash.put("unconfirmedEmail", email);
        }
        Flash.put(request, response, "/auth", flash);
        response.sendRedirect(request.getContextPath() + "/auth");
    }

    /** Erreurs de saisie que le formulaire zod de l'original signalait avant tout appel. */
    private static Optional<String> formError(final String email, final String password) {
        return EmailRules.firstError(email)
                .or(() -> password.isEmpty() ? Optional.of(PASSWORD_REQUIRED) : Optional.empty())
                .or(() -> password.length() > PasswordPolicy.MAX_LENGTH ? Optional.of(PASSWORD_TOO_LONG) : Optional.empty());
    }
}
