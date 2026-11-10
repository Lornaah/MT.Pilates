package fr.mtpilates.team;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.HtmxRequest;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import fr.mtpilates.user.AppRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Écran Équipe (/app/admin/team), réservé à l'owner. */
@Controller
@RequestMapping("/app/admin/team")
public class TeamController {

    private static final String PAGE = "pages/team";
    private static final String LIST_FRAGMENT = "pages/team :: list";

    private final TeamService team;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public TeamController(final TeamService team, final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.team = team;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "") final String q, @RequestParam(defaultValue = "all") final String role,
                       final HttpServletRequest request, final Model model) {
        fillList(model, q, role);
        model.addAttribute("roleOptions", TeamRoles.ASSIGNABLE);
        return HtmxRequest.view(request, PAGE, LIST_FRAGMENT);
    }

    @PostMapping("/{userId}/roles")
    public String addRole(@PathVariable final UUID userId, @RequestParam final String newRole,
                          @RequestParam(defaultValue = "") final String q, @RequestParam(defaultValue = "all") final String role,
                          final HttpServletResponse response, final Model model) {
        AppRole.fromCode(newRole).ifPresent(r -> team.addRole(currentUsers.require(), userId, r));
        toasts.trigger(response, Toast.success("Rôle ajouté"));
        fillList(model, q, role);
        return LIST_FRAGMENT;
    }

    @DeleteMapping("/{userId}/roles/{removed}")
    public String removeRole(@PathVariable final UUID userId, @PathVariable final String removed,
                             @RequestParam(defaultValue = "") final String q, @RequestParam(defaultValue = "all") final String role,
                             final HttpServletResponse response, final Model model) {
        try {
            AppRole.fromCode(removed).ifPresent(r -> team.removeRole(currentUsers.require(), userId, r));
            toasts.trigger(response, Toast.success("Rôle retiré"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
        }
        fillList(model, q, role);
        return LIST_FRAGMENT;
    }

    private void fillList(final Model model, final String search, final String role) {
        final Optional<AppRole> filter = AppRole.fromCode(role);
        model.addAttribute("members", team.list(search, filter));
        model.addAttribute("q", search);
        model.addAttribute("role", role);
    }
}
