package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.HtmxRequest;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** Listes des membres : « Mes coachés » (/app/coach/members) et « Tous les membres » (/app/admin/members). */
@Controller
public class MembersController {

    private static final String PAGE = "pages/members";
    private static final String LIST_FRAGMENT = "pages/members :: list";
    private static final String ADMIN_PATH = "/app/admin/members";
    private static final String CREATE_DIALOG = "create-member";

    private final MemberService members;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public MembersController(final MemberService members, final CurrentUserService currentUsers,
                             final ToastResponses toasts) {
        this.members = members;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping("/app/coach/members")
    public String coachList(@ModelAttribute final MemberFilters filters, final HttpServletRequest request,
                            final Model model) {
        fill(model, false, filters);
        return HtmxRequest.view(request, PAGE, LIST_FRAGMENT);
    }

    @GetMapping(ADMIN_PATH)
    public String adminList(@ModelAttribute final MemberFilters filters, final HttpServletRequest request,
                            final Model model) {
        fill(model, true, filters);
        return HtmxRequest.view(request, PAGE, LIST_FRAGMENT);
    }

    @PostMapping(ADMIN_PATH)
    public String create(@ModelAttribute final CreateMemberForm form, @ModelAttribute final MemberFilters filters,
                         final HttpServletResponse response, final Model model) {
        try {
            members.create(currentUsers.require(), form);
        } catch (final MemberCreationException e) {
            toasts.trigger(response, Toast.error(e.getMessage()));
            response.setHeader("HX-Reswap", "none");
            fill(model, true, filters);
            return LIST_FRAGMENT;
        }
        toasts.trigger(response, Toast.success("Membre créé. Communiquez-lui ses identifiants."),
                Map.of("dialog-close", CREATE_DIALOG));
        fill(model, true, filters);
        return LIST_FRAGMENT;
    }

    /** Dialog de modification, chargé à la demande et ouvert dès son insertion. */
    @GetMapping(ADMIN_PATH + "/{id}/edit")
    public String editDialog(@PathVariable final UUID id, final Model model) {
        model.addAttribute("member", members.editable(currentUsers.require(), id));
        model.addAttribute("coaches", members.coaches());
        model.addAttribute("statuses", MemberStatus.values());
        return "pages/members :: editDialog";
    }

    @PostMapping(ADMIN_PATH + "/{id}")
    public String update(@PathVariable final UUID id, @ModelAttribute final EditMemberForm form,
                         @ModelAttribute final MemberFilters filters, final HttpServletResponse response,
                         final Model model) {
        members.update(currentUsers.require(), id, form);
        toasts.trigger(response, Toast.success("Fiche mise à jour"), Map.of("dialog-close", "edit-member"));
        fill(model, true, filters);
        return LIST_FRAGMENT;
    }

    @DeleteMapping(ADMIN_PATH + "/{id}")
    public String delete(@PathVariable final UUID id, @ModelAttribute final MemberFilters filters,
                         final HttpServletResponse response, final Model model) {
        members.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Membre supprimé"));
        fill(model, true, filters);
        return LIST_FRAGMENT;
    }

    private void fill(final Model model, final boolean admin, final MemberFilters filters) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("admin", admin);
        model.addAttribute("canManage", admin && user.capacities().isOwner());
        model.addAttribute("filters", filters);
        model.addAttribute("coaches", members.coaches());
        model.addAttribute("statuses", MemberStatus.values());
        model.addAttribute("members", admin ? members.visibleTo(user, filters) : members.coachedBy(user, filters));
    }
}
