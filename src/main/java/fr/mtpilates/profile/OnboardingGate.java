package fr.mtpilates.profile;

import fr.mtpilates.auth.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Un membre qui n'a pas terminé l'onboarding est renvoyé vers /onboarding (OnboardingGate de l'original). */
@Component
public class OnboardingGate implements HandlerInterceptor {

    private final CurrentUserService currentUsers;
    private final ProfileService profiles;

    public OnboardingGate(final CurrentUserService currentUsers, final ProfileService profiles) {
        this.currentUsers = currentUsers;
        this.profiles = profiles;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
            throws IOException {
        final boolean redirect = currentUsers.current().map(profiles::needsOnboarding).orElse(false);
        if (redirect) {
            response.sendRedirect(request.getContextPath() + "/onboarding");
        }
        return !redirect;
    }
}
