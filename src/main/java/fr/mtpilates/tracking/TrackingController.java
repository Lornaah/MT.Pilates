package fr.mtpilates.tracking;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
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

/** « Mon suivi » (/app/tracking) : bilan de la semaine, photos d'évolution, mensurations. */
@Controller
@RequestMapping("/app/tracking")
public class TrackingController {

    private static final String PAGE = "pages/tracking";
    private static final String ENTRIES = "pages/tracking :: entries";
    private static final String REVIEWS = "pages/tracking :: reviews";

    private final TrackingService tracking;
    private final WeeklyReviewService reviews;
    private final MemberPhotoService photos;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final Clock clock;

    public TrackingController(final TrackingService tracking, final WeeklyReviewService reviews,
                              final MemberPhotoService photos, final CurrentUserService currentUsers,
                              final ToastResponses toasts, final Clock clock) {
        this.tracking = tracking;
        this.reviews = reviews;
        this.photos = photos;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.clock = clock;
    }

    @GetMapping
    public String page(final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("memberId", user.id());
        model.addAttribute("today", LocalDate.now(clock).toString());
        model.addAttribute("weekStart", reviews.currentWeekStart().toString());
        model.addAttribute("measures", Measure.values());
        model.addAttribute("photos", photos.photos(user, user.id()));
        fillReviews(model, user);
        fillEntries(model, user);
        return PAGE;
    }

    @PostMapping("/entries")
    public String addEntry(@ModelAttribute final TrackingEntryForm form, final HttpServletResponse response,
                           final Model model) {
        final CurrentUser user = currentUsers.require();
        tracking.add(user, form);
        toasts.trigger(response, Toast.success("Entrée ajoutée"), Map.of("dialog-close", "tracking-entry"));
        fillEntries(model, user);
        return ENTRIES;
    }

    @DeleteMapping("/entries/{id}")
    public String deleteEntry(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        tracking.delete(user, id);
        toasts.trigger(response, Toast.success("Supprimé"));
        fillEntries(model, user);
        return ENTRIES;
    }

    @PostMapping("/reviews")
    public String recordReview(@RequestParam(defaultValue = "") final String weekStart,
                               @RequestParam(defaultValue = "") final String score,
                               @RequestParam(defaultValue = "") final String note,
                               final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        reviews.record(user, weekStart, score, note);
        toasts.trigger(response, Toast.success("Bilan enregistré"), Map.of("dialog-close", "weekly-review"));
        fillReviews(model, user);
        return REVIEWS;
    }

    @DeleteMapping("/reviews/{id}")
    public String deleteReview(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        final CurrentUser user = currentUsers.require();
        reviews.delete(user, id);
        toasts.trigger(response, Toast.success("Bilan supprimé"));
        fillReviews(model, user);
        return REVIEWS;
    }

    private void fillEntries(final Model model, final CurrentUser user) {
        model.addAttribute("entries", tracking.entries(user, user.id()));
    }

    private void fillReviews(final Model model, final CurrentUser user) {
        model.addAttribute("reviews", reviews.reviews(user, user.id()));
    }
}
