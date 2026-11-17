package fr.mtpilates.prospect;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.prospect.ProspectViews.Row;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Leads (/app/prospects), Closing (/app/calls) et « Mon lien » (/app/my-booking-link). Les dialogs sont communs
 * aux deux pages : `from=calls` fait re-rendre la page Closing au lieu des leads.
 */
@Controller
public class ProspectController {

    private static final String LEADS = "pages/prospects :: content";
    private static final String CALLS = "pages/calls :: content";

    private final ProspectService prospects;
    private final MemberService members;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public ProspectController(final ProspectService prospects, final MemberService members,
                              final CurrentUserService currentUsers, final ToastResponses toasts) {
        this.prospects = prospects;
        this.members = members;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping("/app/prospects")
    public String leads(final Model model) {
        fillLeads(model);
        return "pages/prospects";
    }

    @GetMapping("/app/admin/prospects")
    public String legacyRedirect() {
        return "redirect:/app/prospects";
    }

    @GetMapping("/app/calls")
    public String calls(final Model model) {
        fillCalls(model);
        return "pages/calls";
    }

    @GetMapping("/app/my-booking-link")
    public String bookingLink() {
        return "pages/my-booking-link";
    }

    @GetMapping({"/app/prospects/new", "/app/prospects/{id}/edit"})
    public String form(@PathVariable(required = false) final UUID id, @RequestParam(defaultValue = "leads") final String from,
                       final Model model) {
        model.addAttribute("form", prospects.formValues(currentUsers.require(), Optional.ofNullable(id)));
        model.addAttribute("from", from);
        model.addAttribute("sources", ProspectService.SOURCES);
        model.addAttribute("statuses", ProspectService.STATUSES);
        return "fragments/prospect/dialogs :: prospectDialog";
    }

    @PostMapping({"/app/prospects", "/app/prospects/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @ModelAttribute final ProspectForm form,
                       @RequestParam(defaultValue = "leads") final String from, final HttpServletResponse response,
                       final Model model) {
        try {
            prospects.save(currentUsers.require(), Optional.ofNullable(id), form);
            toasts.trigger(response, Toast.success(id == null ? "Prospect ajouté" : "Prospect mis à jour"),
                    Map.of("dialog-close", "prospect-form"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        return refresh(from, model);
    }

    @PostMapping("/app/prospects/{id}/status")
    public String status(@PathVariable final UUID id, @RequestParam final String status, final Model model) {
        prospects.changeStatus(currentUsers.require(), id, status);
        fillLeads(model);
        return LEADS;
    }

    @DeleteMapping("/app/prospects/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        prospects.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Prospect supprimé"));
        fillLeads(model);
        return LEADS;
    }

    @GetMapping("/app/prospects/{id}/convert")
    public String convertDialog(@PathVariable final UUID id, @RequestParam(defaultValue = "leads") final String from,
                                final Model model) {
        model.addAttribute("prospect", prospects.find(currentUsers.require(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("coaches", members.coaches());
        model.addAttribute("from", from);
        return "fragments/prospect/dialogs :: convertDialog";
    }

    @PostMapping("/app/prospects/{id}/convert")
    public String convert(@PathVariable final UUID id, @ModelAttribute final ConvertForm form,
                          @RequestParam(defaultValue = "leads") final String from, final HttpServletResponse response,
                          final Model model) {
        try {
            prospects.convert(currentUsers.require(), id, form);
            toasts.trigger(response, Toast.success("Prospect converti en membre !"), Map.of("dialog-close", "prospect-convert"));
        } catch (final IllegalArgumentException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
        }
        return refresh(from, model);
    }

    private String refresh(final String from, final Model model) {
        if ("calls".equals(from)) {
            fillCalls(model);
            return CALLS;
        }
        fillLeads(model);
        return LEADS;
    }

    private void fillLeads(final Model model) {
        final CurrentUser user = currentUsers.require();
        final List<Row> rows = prospects.list(user);
        model.addAttribute("prospects", rows);
        model.addAttribute("stats", prospects.stats(rows));
        model.addAttribute("columns", prospects.columns(rows));
        model.addAttribute("sources", prospects.sources(rows));
        model.addAttribute("statuses", ProspectService.STATUSES);
    }

    private void fillCalls(final Model model) {
        model.addAttribute("calls", prospects.calls(currentUsers.require()));
    }
}
