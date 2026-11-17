package fr.mtpilates.training;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** « Mon programme » (/app/planning) : aujourd'hui, cette semaine, vue d'ensemble ; jours marqués faits. */
@Controller
public class PlanningController {

    private final PlanningService planning;
    private final CurrentUserService currentUsers;

    public PlanningController(final PlanningService planning, final CurrentUserService currentUsers) {
        this.planning = planning;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/app/planning")
    public String page(final Model model) {
        planning.active(currentUsers.require()).ifPresent(p -> model.addAttribute("planning", p));
        return "pages/planning";
    }

    @PostMapping("/app/planning/days/{dayId}/toggle")
    public String toggle(@PathVariable final UUID dayId, final Model model) {
        final CurrentUser user = currentUsers.require();
        planning.toggleDone(user, dayId);
        planning.active(user).ifPresent(p -> model.addAttribute("planning", p));
        return "pages/planning :: planningBody";
    }
}
