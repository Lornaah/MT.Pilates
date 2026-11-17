package fr.mtpilates.training;

import fr.mtpilates.auth.CurrentUserService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Hub « Programme » (/app/coach/training?tab=programs|sessions|exercises) : un onglet par bibliothèque. */
@Controller
public class TrainingHubController {

    private static final List<String> TABS = List.of("programs", "sessions", "exercises");

    private final ExerciseService exercises;
    private final CurrentUserService currentUsers;

    public TrainingHubController(final ExerciseService exercises, final CurrentUserService currentUsers) {
        this.exercises = exercises;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/app/coach/training")
    public String hub(@RequestParam(defaultValue = "programs") final String tab, final Model model) {
        final String active = TABS.contains(tab) ? tab : "programs";
        model.addAttribute("tab", active);
        if ("exercises".equals(active)) {
            model.addAttribute("library", exercises.library(currentUsers.require()));
        }
        return "pages/training";
    }
}
