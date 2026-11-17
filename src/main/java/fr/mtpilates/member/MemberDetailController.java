package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Fiche membre côté staff (/app/coach/members/{id}) : onglets Suivi, Coaching, Gestion. */
@Controller
@RequestMapping("/app/coach/members/{id}")
public class MemberDetailController {

    private final MemberService members;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;

    public MemberDetailController(final MemberService members, final CurrentUserService currentUsers,
                                  final ToastResponses toasts) {
        this.members = members;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
    }

    @GetMapping
    public String page(@PathVariable final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        fill(model, user, id);
        model.addAttribute("coaches", members.coaches());
        model.addAttribute("statuses", MemberStatus.values());
        model.addAttribute("isManager", user.capacities().isManager());
        return "pages/member-detail";
    }

    /** Réponse : le résumé re-rendu (lecture et formulaire) avec les valeurs enregistrées. */
    @PostMapping
    public String save(@PathVariable final UUID id, @ModelAttribute final MemberDetailForm form,
                       final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        members.saveDetail(user, id, form);
        toasts.trigger(response, Toast.success("Fiche mise à jour"));
        fill(model, user, id);
        return "pages/member-detail :: summary";
    }

    private void fill(final Model model, final CurrentUser user, final UUID id) {
        model.addAttribute("member", members.detail(user, id));
    }
}
