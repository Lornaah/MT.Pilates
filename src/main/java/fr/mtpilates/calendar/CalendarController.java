package fr.mtpilates.calendar;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.HtmxRequest;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Calendrier du staff (/app/calendar). Onglets et vues (liste / mois) sont un état d'affichage ; le mois affiché
 * et le filtre coach sont des paramètres, renvoyés à chaque action pour re-rendre le même état.
 */
@Controller
@RequestMapping("/app/calendar")
public class CalendarController {

    private static final String PAGE = "pages/calendar";
    private static final String BODY = "pages/calendar :: calendarBody";

    private final CalendarService calendar;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public CalendarController(final CalendarService calendar, final CurrentUserService currentUsers,
                              final ToastResponses toasts) {
        this.calendar = calendar;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(@ModelAttribute final CalendarState state, final HttpServletRequest request, final Model model) {
        fill(model, state);
        return HtmxRequest.view(request, PAGE, BODY);
    }

    @GetMapping("/day")
    public String day(@RequestParam final String date, @RequestParam(defaultValue = "mine") final String scope,
                      @ModelAttribute final CalendarState state, final Model model) {
        final CurrentUser user = currentUsers.require();
        final List<CalendarViews.EventView> source = "general".equals(scope)
                ? calendar.general(user, state.coachId()) : calendar.mine(user);
        model.addAttribute("date", date);
        model.addAttribute("dateTitle", dayTitle(date));
        model.addAttribute("dayEvents", calendar.onDay(source, date));
        model.addAttribute("showCoach", "general".equals(scope));
        return "pages/calendar :: dayDialog";
    }

    @GetMapping("/new")
    public String newEvent(@RequestParam(defaultValue = "") final String start, final Model model) {
        model.addAttribute("start", start);
        model.addAttribute("memberOptions", calendar.memberOptions(currentUsers.require()));
        return "pages/calendar :: eventDialog";
    }

    @PostMapping("/events")
    public String create(@ModelAttribute final EventForm form, @ModelAttribute final CalendarState state,
                         final HttpServletResponse response, final Model model) {
        try {
            calendar.create(currentUsers.require(), form);
            toasts.trigger(response, Toast.success("Événement créé"), Map.of("dialog-close", "event-form"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        fill(model, state);
        return BODY;
    }

    @DeleteMapping("/events/{id}")
    public String delete(@PathVariable final UUID id, @ModelAttribute final CalendarState state,
                         final HttpServletResponse response, final Model model) {
        calendar.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Supprimé"), Map.of("dialog-close", "calendar-day"));
        fill(model, state);
        return BODY;
    }

    private void fill(final Model model, final CalendarState state) {
        final CurrentUser user = currentUsers.require();
        final List<CalendarViews.EventView> mine = calendar.mine(user);
        model.addAttribute("state", state);
        model.addAttribute("isOwner", user.isRealOwner());
        model.addAttribute("mineByDay", calendar.byDay(mine));
        model.addAttribute("mineMonth", calendar.month(mine, state.myMonth()));
        if (user.isRealOwner()) {
            final List<CalendarViews.EventView> general = calendar.general(user, state.coachId());
            model.addAttribute("generalByDay", calendar.byDay(general));
            model.addAttribute("generalMonth", calendar.month(general, state.genMonth()));
            model.addAttribute("staff", calendar.staff());
        }
    }

    private static String dayTitle(final String iso) {
        try {
            return LocalDate.parse(iso).format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.FRENCH));
        } catch (final DateTimeParseException e) {
            return "";
        }
    }

    /** État d'affichage renvoyé par la page : mois de chaque vue et coach filtré. */
    public record CalendarState(String myMonth, String genMonth, String coach) {

        public Optional<UUID> coachId() {
            try {
                return coach == null || coach.isBlank() || "all".equals(coach) ? Optional.empty() : Optional.of(UUID.fromString(coach));
            } catch (final IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        public String coachOrAll() {
            return coachId().map(UUID::toString).orElse("all");
        }
    }
}
