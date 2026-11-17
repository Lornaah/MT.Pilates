package fr.mtpilates.announcement;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.CurrentUserService;
import fr.mtpilates.member.MemberFilters;
import fr.mtpilates.member.MemberService;
import fr.mtpilates.shared.ui.Markdown;
import fr.mtpilates.shared.web.Toast;
import fr.mtpilates.shared.web.ToastResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** Annonces : gestion (/app/coach/announcements), lecture (/app/announcements), cloche. */
@Controller
public class AnnouncementController {

    private static final String TABS = "pages/announcements-staff :: tabs";

    private final AnnouncementService announcements;
    private final MemberService members;
    private final CurrentUserService currentUsers;
    private final ToastResponses toasts;
    private final Markdown markdown;

    public AnnouncementController(final AnnouncementService announcements, final MemberService members,
                                  final CurrentUserService currentUsers,
                                  final ToastResponses toasts, final Markdown markdown) {
        this.announcements = announcements;
        this.members = members;
        this.currentUsers = currentUsers;
        this.toasts = toasts;
        this.markdown = markdown;
    }

    @GetMapping("/app/coach/announcements")
    public String staffPage(final Model model) {
        model.addAttribute("groups", announcements.staff(currentUsers.require()));
        return "pages/announcements-staff";
    }

    @GetMapping({"/app/coach/announcements/new", "/app/coach/announcements/{id}/edit"})
    public String formDialog(@PathVariable(required = false) final UUID id, final Model model) {
        final CurrentUser user = currentUsers.require();
        model.addAttribute("form", announcements.formValues(user, Optional.ofNullable(id)));
        model.addAttribute("members", members.visibleTo(user, new MemberFilters("", MemberFilters.ALL, MemberFilters.ALL)));
        model.addAttribute("coaches", members.coaches());
        return "pages/announcements-staff :: announcementDialog";
    }

    @PostMapping({"/app/coach/announcements", "/app/coach/announcements/{id}"})
    public String save(@PathVariable(required = false) final UUID id, @ModelAttribute final AnnouncementForm form,
                       final HttpServletResponse response, final Model model) {
        if (announcements.save(currentUsers.require(), Optional.ofNullable(id), form)) {
            toasts.trigger(response, Toast.success(id == null ? "Annonce enregistrée" : "Annonce mise à jour"),
                    Map.of("dialog-close", "announcement-form"));
        } else {
            response.setHeader("HX-Reswap", "none");
        }
        model.addAttribute("groups", announcements.staff(currentUsers.require()));
        return TABS;
    }

    @DeleteMapping("/app/coach/announcements/{id}")
    public String delete(@PathVariable final UUID id, final HttpServletResponse response, final Model model) {
        announcements.delete(currentUsers.require(), id);
        toasts.trigger(response, Toast.success("Annonce supprimée"));
        model.addAttribute("groups", announcements.staff(currentUsers.require()));
        return TABS;
    }

    @PostMapping("/app/coach/announcements/preview")
    public ResponseEntity<String> preview(@RequestParam(defaultValue = "") final String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body(markdown.toHtml(body.isBlank() ? "*Vide*" : body));
    }

    @PostMapping("/app/coach/announcements/cover")
    public String uploadCover(@RequestParam(required = false) final MultipartFile file, final Model model)
            throws IOException {
        model.addAttribute("coverUrl", "");
        if (file != null && !file.isEmpty()) {
            try (InputStream content = file.getInputStream()) {
                model.addAttribute("coverUrl", announcements.uploadCover(currentUsers.require(), file.getOriginalFilename(),
                        file.getContentType(), content));
            }
        }
        return "pages/announcements-staff :: cover(coverUrl=${coverUrl})";
    }

    @GetMapping("/app/announcements")
    public String memberPage(final Model model) {
        model.addAttribute("items", announcements.forUser(currentUsers.require()));
        return "pages/announcements";
    }

    @PostMapping("/app/announcements/{id}/read")
    public String markRead(@PathVariable final UUID id, @RequestParam(defaultValue = "page") final String from,
                           final Model model) {
        final CurrentUser user = currentUsers.require();
        announcements.markRead(user, id);
        if ("bell".equals(from)) {
            model.addAttribute("bell", announcements.bell(user));
            model.addAttribute("bellOpen", true);
            return "fragments/app/notification-bell :: bell";
        }
        model.addAttribute("items", announcements.forUser(user));
        return "pages/announcements :: list";
    }
}
