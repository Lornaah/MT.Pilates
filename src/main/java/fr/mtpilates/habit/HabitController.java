package fr.mtpilates.habit;

import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Habitudes : bibliothèque du staff (/app/coach/habits), cases du jour du membre, suivi sur la fiche. */
@Controller
public class HabitController {

    private static final String GRID = "pages/habits :: grid";

    private final HabitService habits;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public HabitController(final HabitService habits, final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.habits = habits;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping("/app/coach/habits")
    public String page(final Model model) {
        model.addAttribute("habits", habits.library(currentUsers.require()));
        return "pages/habits";
    }

    @GetMapping({"/app/coach/habits/new", "/app/coach/habits/{id}/edit"})
    public String formDialog(@PathVariable(required = false) final UUID id, final Model model) {
        model.addAttribute("habit", Optional.ofNullable(id).flatMap(i -> habits.find(currentUsers.require(), i)).orElse(null));
        model.addAttribute("iconNames", HabitIcons.names());
        model.addAttribute("habitIcons", HabitIcons.all());
        return "pages/habits :: habitDialog";
    }

    @PostMapping({"/app/coach/habits", "/app/coach/habits/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @ModelAttribute final HabitForm form,
                       final HttpServletResponse response, final Model model) {
        if (habits.save(currentUsers.require(), Optional.ofNullable(id), form)) {
            toasts.trigger(response, Toast.success(id == null ? "Habitude créée" : "Habitude mise à jour"),
                    Map.of("dialog-close", "habit-form"));
        } else {
            response.setHeader("HX-Reswap", "none");
        }
        return grid(model);
    }

    @DeleteMapping("/app/coach/habits/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        habits.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Habitude supprimée"));
        return grid(model);
    }

    @GetMapping("/app/coach/habits/{id}/assign")
    public String assignDialog(@PathVariable final UUID id, final Model model) {
        model.addAttribute("habit", habits.find(currentUsers.require(), id).orElseThrow());
        model.addAttribute("assignees", habits.assignees(currentUsers.require(), id));
        return "pages/habits :: assignDialog";
    }

    @PostMapping("/app/coach/habits/{id}/assign")
    public String assign(@PathVariable final UUID id, @RequestParam(required = false) final List<UUID> userIds,
                         final HttpServletResponse response, final Model model) {
        habits.setAssignments(currentUsers.require(), id, Optional.ofNullable(userIds).orElse(List.of()));
        toasts.trigger(response, Toast.success("Assignations mises à jour"), Map.of("dialog-close", "habit-assign"));
        return grid(model);
    }

    @PostMapping("/app/habits/{id}/toggle")
    public String toggle(@PathVariable final UUID id, final Model model) {
        habits.toggleToday(currentUsers.require(), id);
        model.addAttribute("myHabits", habits.today(currentUsers.require()));
        return "fragments/habit/my-habits :: card";
    }

    @GetMapping("/app/members/{memberId}/habits")
    public String tracking(@PathVariable final UUID memberId, @RequestParam(defaultValue = "30") final int range,
                           final Model model) {
        model.addAttribute("habitTracking", habits.tracking(currentUsers.require(), memberId, range));
        model.addAttribute("memberId", memberId);
        return "fragments/habit/tracking :: card";
    }

    private String grid(final Model model) {
        model.addAttribute("habits", habits.library(currentUsers.require()));
        return GRID;
    }
}
