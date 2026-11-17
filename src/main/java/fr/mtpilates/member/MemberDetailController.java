package fr.mtpilates.member;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import fr.mtpilates.tracking.MemberPhotoService;
import fr.mtpilates.tracking.TrackingChartService;
import java.time.Clock;
import java.time.LocalDate;
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
    private final MemberDocumentService documents;
    private final MemberPhotoService photos;
    private final TrackingChartService chart;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final Clock clock;

    public MemberDetailController(final MemberService members, final MemberDocumentService documents,
                                  final MemberPhotoService photos, final TrackingChartService chart,
                                  final CurrentUserService currentUsers, final ToastResponses toasts, final Clock clock) {
        this.members = members;
        this.documents = documents;
        this.photos = photos;
        this.chart = chart;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.clock = clock;
    }

    @GetMapping
    public String page(@PathVariable final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        fill(model, user, id);
        model.addAttribute("coaches", members.coaches());
        model.addAttribute("statuses", MemberStatus.values());
        model.addAttribute("isManager", user.capacities().isManager());
        model.addAttribute("memberId", id);
        model.addAttribute("today", LocalDate.now(clock).toString());
        model.addAttribute("photos", photos.photos(user, id));
        model.addAttribute("documents", documents.documents(user, id));
        model.addAttribute("chartPoints", chart.points(user, id));
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
