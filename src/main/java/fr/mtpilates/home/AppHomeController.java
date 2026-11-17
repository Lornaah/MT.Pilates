package fr.mtpilates.home;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.habit.HabitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Accueil de l'espace connecté (DashboardRouter) : tableau de bord staff ou membre selon les capacités. */
@Controller
public class AppHomeController {

    private final DashboardService dashboards;
    private final HabitService habits;
    private final CurrentUserService currentUsers;

    public AppHomeController(final DashboardService dashboards, final HabitService habits,
                             final CurrentUserService currentUsers) {
        this.dashboards = dashboards;
        this.habits = habits;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/app")
    public String home(final Model model) {
        final CurrentUser user = currentUsers.require();
        if (user.capacities().isStaff()) {
            model.addAttribute("staffDashboard", dashboards.staff(user));
        } else {
            model.addAttribute("memberDashboard", dashboards.member(user));
            model.addAttribute("myHabits", habits.today(user));
        }
        return "pages/app-home";
    }
}
