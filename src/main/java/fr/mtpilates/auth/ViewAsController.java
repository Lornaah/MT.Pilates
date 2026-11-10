package fr.mtpilates.auth;

import fr.mtpilates.user.AppRole;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** « Voir comme… » de l'owner : rôle endossé pour la session, puis retour sur /app. */
@Controller
public class ViewAsController {

    private final CurrentUserService currentUsers;

    public ViewAsController(final CurrentUserService currentUsers) {
        this.currentUsers = currentUsers;
    }

    @PostMapping("/app/view-as")
    public String viewAs(@RequestParam(defaultValue = "owner") final String role, final HttpSession session) {
        if (currentUsers.require().isRealOwner()) {
            currentUsers.setViewAs(session, AppRole.fromCode(role).flatMap(ViewAsRole::of));
        }
        return "redirect:/app";
    }
}
